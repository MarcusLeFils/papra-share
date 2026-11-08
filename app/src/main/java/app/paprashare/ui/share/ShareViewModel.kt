package app.paprashare.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paprashare.data.PapraApi
import app.paprashare.data.SettingsRepository
import app.paprashare.domain.PapraErrorKind
import app.paprashare.domain.Organization
import app.paprashare.domain.UploadResult
import app.paprashare.domain.defaultOrganizationId
import app.paprashare.domain.parseOrganizations
import app.paprashare.domain.serializeOrganizations
import app.paprashare.util.SharedDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * État de l'écran de partage.
 *
 * Le ViewModel ne porte que des DONNÉES pures — il ne connaît aucune ressource de
 * chaîne. Le mapping `PapraErrorKind → R.string` vit dans la couche écran
 * (UI owns strings), qui seule comprend la localisation.
 */
sealed class ShareUiState {
    object Loading : ShareUiState()

    /** Prêt à envoyer. @param isConfigured réglages complets (URL + clé + organisation sauvegardée). */
    data class Ready(val isConfigured: Boolean) : ShareUiState()
    object Uploading : ShareUiState()
    data class Success(val count: Int) : ShareUiState()

    /**
     * Échec d'envoi.
     * @param kind      cause (jamais null : au moins un échec existe ici)
     * @param fileName  document fautif (affiché par la ressource si pertinent)
     * @param failures  nombre d'échecs (0 si un seul envoi)
     * @param total     nombre total de documents tentés
     * @param details   corps brut, réservé au bouton « Détails »
     */
    data class Error(
        val kind: PapraErrorKind,
        val fileName: String? = null,
        val failures: Int = 0,
        val total: Int = 0,
        val details: String? = null,
    ) : ShareUiState()
}

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val api = PapraApi()

    private val _state = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val state: StateFlow<ShareUiState> = _state

    /** Documents à partager — détenus une seule fois ici (source de vérité). */
    private var _documents: List<SharedDocument> = emptyList()
    val documents: List<SharedDocument> get() = _documents

    /** Statut de configuration, recalculé à chaque init (lecture synchrone). */
    private var _configured = false

    /** Organisations disponibles (obtenues du réseau ou du cache). */
    private val _organizations = MutableStateFlow<List<Organization>>(emptyList())
    val organizations: StateFlow<List<Organization>> = _organizations

    /** Organisation sélectionnée pour le partage en cours — source de vérité de la cible. */
    private val _selectedOrganizationId = MutableStateFlow("")
    val selectedOrganizationId: StateFlow<String> = _selectedOrganizationId

    /**
     * Remplace la liste de documents. Appelé à la création et à chaque nouvelle
     * intention de partage : idempotent, et remplace TOUJOURS la liste courante
     * (sinon un second partage resterait figé sur les anciens documents).
     *
     * L'écran passe à Ready IMMÉDIATEMENT depuis le cache : la requête réseau de
     * rafraîchissement de la liste court en arrière-plan et ne doit JAMAIS geler
     * le partage (hors-ligne ou réseau lent → l'envoi reste possible vers
     * l'organisation sauvegardée). Quand la liste fraîche arrive, on la prend si
     * l'utilisateur n'a pas déjà choisi une organisation qui a disparu.
     */
    fun init(documents: List<SharedDocument>) {
        if (_state.value is ShareUiState.Uploading) return
        _documents = documents
        _state.value = ShareUiState.Loading
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _configured = settings.isConfigured

            val cached = parseOrganizations(settingsRepository.organizationsCache.first())
            val lastUsedId = settingsRepository.lastOrganizationId.first().takeIf { it.isNotBlank() }
            _organizations.value = cached
            _selectedOrganizationId.value = defaultOrganizationId(
                cachedIds = cached.map { it.id },
                savedId = settings.organizationId,
                lastUsedId = lastUsedId,
            ) ?: settings.organizationId
            // Prêt dès le cache : le réseau ne bloque jamais.
            _state.value = ShareUiState.Ready(_configured)

            // Rafraîchissement best-effort en arrière-plan (si configuré).
            if (_configured) {
                try {
                    val fresh = api.getOrganizations(settings)
                    if (fresh.isNotEmpty()) {
                        settingsRepository.setOrganizationsCache(serializeOrganizations(fresh))
                        _organizations.value = fresh
                        // Préserve le choix en cours s'il est toujours valide, sinon recalcule.
                        val current = _selectedOrganizationId.value
                        _selectedOrganizationId.value = current.takeIf { id -> fresh.any { it.id == id } }
                            ?: defaultOrganizationId(
                                cachedIds = fresh.map { it.id },
                                savedId = settings.organizationId,
                                lastUsedId = lastUsedId,
                            ) ?: settings.organizationId
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Liste indisponible → on garde le cache (éventuellement vide) ;
                    // le repli sur l'organisation sauvegardée reste en place.
                }
            }
        }
    }

    /** L'utilisateur choisit l'organisation cible du partage en cours. */
    fun selectOrganization(id: String) {
        _selectedOrganizationId.value = id
        viewModelScope.launch { settingsRepository.setLastOrganizationId(id) }
    }

    /**
     * Lance l'envoi des documents courants. Sans argument : source unique = _documents.
     * L'écran n'expose le bouton d'upload que si `isConfigured` — les gardes « aucun
     * document » et « non configuré » sont donc défensives et restent sans message.
     */
    fun upload() {
        if (_state.value is ShareUiState.Uploading) return
        val toSend = _documents
        if (toSend.isEmpty() || !_configured) return

        // Anti-réentrance : basculer AVANT tout await (un second appui ne doit
        // pas lancer une coroutine concurrente, sinon double envoi).
        _state.value = ShareUiState.Uploading

        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val context = getApplication<Application>()

            // La cible est le choix du moment (sélecteur) ; à défaut, ID sauvegardé.
            val orgId = _selectedOrganizationId.value.ifBlank { settings.organizationId }
            val settingsForUpload = settings.copy(organizationId = orgId)

            // Upload séquentiel : chaque échec est annoté du nom du document fautif.
            val results = toSend.map { doc ->
                val stream = runCatching {
                    context.contentResolver.openInputStream(doc.uri)
                }.getOrNull()

                val result = if (stream == null) {
                    UploadResult.Failure(kind = PapraErrorKind.OPEN_FILE)
                } else {
                    api.uploadDocument(
                        settings = settingsForUpload,
                        fileName = doc.displayName,
                        mimeType = doc.mimeType,
                        data = stream,
                    )
                }
                // Tous les échecs portent le nom du fichier pour l'UI.
                if (result is UploadResult.Failure) {
                    UploadResult.Failure(result.kind, doc.displayName, result.details)
                } else {
                    result
                }
            }

            val successes = results.count { it is UploadResult.Success }
            val firstFailure = results.firstOrNull { it is UploadResult.Failure }
                as? UploadResult.Failure
            val total = toSend.size
            val failures = total - successes

            // Ne mémorise le choix que si le document l'atteint effectivement :
            // un échec complet ne doit pas entériner une cible comme « dernière utilisée ».
            if (successes > 0) settingsRepository.setLastOrganizationId(orgId)

            if (successes == total) {
                _state.value = ShareUiState.Success(total)
            } else {
                // firstFailure est toujours non-null ici (successes < total).
                _state.value = ShareUiState.Error(
                    kind = firstFailure!!.kind,
                    fileName = firstFailure!!.fileName,
                    failures = failures,
                    total = total,
                    details = firstFailure!!.details,
                )
            }
        }
    }
}
