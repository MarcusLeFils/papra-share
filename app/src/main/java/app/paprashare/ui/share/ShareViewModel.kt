package app.paprashare.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paprashare.data.PapraApi
import app.paprashare.data.SettingsRepository
import app.paprashare.domain.PapraErrorKind
import app.paprashare.domain.UploadResult
import app.paprashare.util.SharedDocument
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

    /**
     * Remplace la liste de documents. Appelé à la création et à chaque nouvelle
     * intention de partage : idempotent, et remplace TOUJOURS la liste courante
     * (sinon un second partage resterait figé sur les anciens documents).
     */
    fun init(documents: List<SharedDocument>) {
        if (_state.value is ShareUiState.Uploading) return
        _documents = documents
        _state.value = ShareUiState.Loading
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _configured = settings.isConfigured
            _state.value = ShareUiState.Ready(_configured)
        }
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

            // Upload séquentiel : chaque échec est annoté du nom du document fautif.
            val results = toSend.map { doc ->
                val stream = runCatching {
                    context.contentResolver.openInputStream(doc.uri)
                }.getOrNull()

                val result = if (stream == null) {
                    UploadResult.Failure(kind = PapraErrorKind.OPEN_FILE)
                } else {
                    api.uploadDocument(
                        settings = settings,
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