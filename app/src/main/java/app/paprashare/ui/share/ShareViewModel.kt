package app.paprashare.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paprashare.R
import app.paprashare.data.PapraApi
import app.paprashare.data.PapraErrorKind
import app.paprashare.data.SettingsRepository
import app.paprashare.data.UploadResult
import app.paprashare.util.SharedDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * État de l'écran de partage.
 *
 * Tous les messages sont portés par un identifiant de ressource (`@StringRes`) et
 * d'éventuels arguments, résolus côté UI via `stringResource` — jamais de texte
 * en dur, pour permettre la localisation.
 */
sealed class ShareUiState {
    object Loading : ShareUiState()
    data class Ready(val isConfigured: Boolean) : ShareUiState()
    data class Uploading(val count: Int) : ShareUiState()
    data class Success(val count: Int) : ShareUiState()
    data class Error(
        val messageRes: Int,
        val messageArgs: List<Any> = emptyList(),
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
            _state.value = ShareUiState.Error(R.string.share_error_no_documents)
            return
        }
        if (!_configured) {
            _state.value = ShareUiState.Error(R.string.share_not_configured)
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
                    UploadResult.Failure(
                        kind = PapraErrorKind.OPEN_FILE,
                        details = doc.displayName,
                    )
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
                _state.value = ShareUiState.Success(total)
            } else {
                val (res, args) = firstFailure?.let { messageResOf(it, successes, total) }
                    ?: (R.string.share_error_upload_failed to emptyList<Any>())
                _state.value = ShareUiState.Error(
                    messageRes = res,
                    messageArgs = args,
                    details = firstFailure?.details,
                )
            }
        }
    }

    /** Résout la clé de ressource + arguments d'un échec. */
    private fun messageResOf(
        failure: UploadResult.Failure,
        successes: Int,
        total: Int,
    ): Pair<Int, List<Any>> {
        val res = when (failure.kind) {
            PapraErrorKind.NETWORK -> R.string.error_network
            PapraErrorKind.INVALID_RESPONSE -> R.string.error_invalid_response
            PapraErrorKind.BAD_REQUEST -> R.string.error_bad_request
            PapraErrorKind.UNAUTHORIZED -> R.string.error_unauthorized
            PapraErrorKind.FORBIDDEN -> R.string.error_forbidden
            PapraErrorKind.NOT_FOUND -> R.string.error_not_found
            PapraErrorKind.DUPLICATE -> R.string.error_duplicate
            PapraErrorKind.TOO_LARGE -> R.string.error_too_large
            PapraErrorKind.RATE_LIMITED -> R.string.error_rate_limited
            PapraErrorKind.SERVER_ERROR -> R.string.error_server
            PapraErrorKind.HTTP -> R.string.error_http
            PapraErrorKind.OPEN_FILE -> R.string.share_error_open_file
        }

        // Le nom du fichier illisible est le seul argument nécessaire.
        val args = if (failure.kind == PapraErrorKind.OPEN_FILE) {
            listOf(failure.details.orEmpty())
        } else {
            emptyList<Any>()
        }

        // Suffixe de progression partielle (ex. « (2/3 envoyés) ») quand certains uploads
        // ont réussi mais pas tous. Cet état est encodé par une ressource dédiée.
        if (successes in 1 until total) {
            return Pair(R.string.share_error_partial_upload, listOf(successes, total))
        }
        return Pair(res, args)
    }
}