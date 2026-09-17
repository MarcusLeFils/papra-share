package app.paprashare.domain

/**
 * Catégorie d'un échec d'upload, indépendante de la locale et de la couche réseau.
 * La couche UI la mappe vers une ressource de chaîne (`R.string`).
 *
 * `OPEN_FILE` et `INVALID_RESPONSE` n'ont rien de réseau : ils sont produits par
 * d'autres producteurs (ouverture de fichier, contrat de réponse), c'est pourquoi
 * cette énumération vit dans le domaine, non dans `PapraApi`.
 */
enum class PapraErrorKind {
    NETWORK,
    INVALID_RESPONSE,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    DUPLICATE,
    TOO_LARGE,
    RATE_LIMITED,
    SERVER_ERROR,
    HTTP,
    OPEN_FILE,
}

/** Résultat d'un upload. */
sealed class UploadResult {
    data class Success(val documentId: String) : UploadResult()
    data class Failure(
        val kind: PapraErrorKind,
        val fileName: String? = null,
        val details: String? = null,
    ) : UploadResult()
}