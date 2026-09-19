package app.paprashare.domain

/**
 * Normalisation de l'URL d'instance Papra : tronque les espaces et un éventuel
 * slash final, pour construire des requêtes cohérentes.
 */
fun normalizeInstanceUrl(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.endsWith("/")) trimmed.substring(0, trimmed.length - 1) else trimmed
}