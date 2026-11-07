package app.paprashare.domain

/**
 * Règle de sélection par défaut de l'organisation au moment du partage.
 *
 * Ordre de préférence :
 *  1. le dernier choix mémorisé, s'il figure dans la liste ;
 *  2. sinon l'ID configuré dans les réglages, s'il figure dans la liste ;
 *  3. sinon la première organisation de la liste ;
 *  4. sinon `null` (liste vide) → l'appelant replie sur l'ID sauvegardé.
 *
 * Fonction pure et testable en JVM, isolée de tout état Android.
 */
fun defaultOrganizationId(
    cachedIds: List<String>,
    savedId: String,
    lastUsedId: String?,
): String? = listOfNotNull(lastUsedId, savedId)
    .firstOrNull { it in cachedIds }
    ?: cachedIds.firstOrNull()