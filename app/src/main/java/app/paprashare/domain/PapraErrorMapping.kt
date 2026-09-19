package app.paprashare.domain

/**
 * Logique pure de correspondance code HTTP → catégorie d'échec, isolée du client
 * réseau afin d'être testable en JVM sans OkHttp ni Android.
 */
fun httpToErrorKind(code: Int): PapraErrorKind = when (code) {
    400 -> PapraErrorKind.BAD_REQUEST
    401 -> PapraErrorKind.UNAUTHORIZED
    403 -> PapraErrorKind.FORBIDDEN
    404 -> PapraErrorKind.NOT_FOUND
    409 -> PapraErrorKind.DUPLICATE
    413 -> PapraErrorKind.TOO_LARGE
    429 -> PapraErrorKind.RATE_LIMITED
    in 500..599 -> PapraErrorKind.SERVER_ERROR
    else -> PapraErrorKind.HTTP
}