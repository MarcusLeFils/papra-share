package app.paprashare.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paprashare.data.PapraApi
import app.paprashare.data.PapraSettings
import app.paprashare.data.SettingsRepository
import app.paprashare.data.UploadResult
import app.paprashare.util.SharedDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** État de l'écran de partage. */
sealed class ShareUiState {
    object Loading : ShareUiState()
    data class Ready(val isConfigured: Boolean) : ShareUiState()
    data class Uploading(val count: Int) : ShareUiState()
    data class Success(val message: String) : ShareUiState()
    data class Error(
        val message: String,
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

    /** Lance l'envoi des documents courants. Sans argument : source unique = _documents. */
    fun upload() {
        if (_state.value is ShareUiState.Uploading) return
        val toSend = _documents

        if (toSend.isEmpty()) {
            _state.value = ShareUiState.Error("Aucun document à envoyer.")
            return
        }
        if (!_configured) {
            _state.value = ShareUiState.Error(
                "Papra n'est pas configuré. Ouvrez l'application pour renseigner l'URL, la clé API et l'ID d'organisation.",
            )
            return
        }

        // Anti-réentrance : basculer AVANT tout await (un second appui ne doit
        // pas lancer une coroutine concurrente, sinon double envoi).
        _state.value = ShareUiState.Uploading(toSend.size)

        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val context = getApplication<Application>()

            val results = toSend.map { doc ->
                val stream = runCatching {
                    context.contentResolver.openInputStream(doc.uri)
                }.getOrNull()

                if (stream == null) {
                    UploadResult.Failure("Impossible d'ouvrir ${doc.displayName}")
                } else {
                    api.uploadDocument(
                        settings = settings,
                        fileName = doc.displayName,
                        mimeType = doc.mimeType,
                        data = stream,
                    )
                }
            }

            val successes = results.count { it is UploadResult.Success }
            val firstFailure = results.firstOrNull { it is UploadResult.Failure }
                as? UploadResult.Failure

            val total = toSend.size
            if (successes == total) {
                _state.value = ShareUiState.Success(
                    if (total == 1) "Document envoyé sur Papra."
                    else "Documents envoyés : $total."
                )
            } else {
                val partial = if (successes > 0) " ($successes/$total envoyés)" else ""
                val base = firstFailure?.message ?: "Échec de l'envoi."
                _state.value = ShareUiState.Error(
                    message = base + partial,
                    details = firstFailure?.details,
                )
            }
        }
    }
}