package app.paprashare.domain

/**
 * Règle de sélection par défaut de l'organisation au moment du partage.
 *
 * Ordre de préférence :
 *  1. le dernier choix mémorisé, s'il figure dans la liste ;
 *  2. sinon l'ID configuré dans les réglages, s'il figure dans la liste ;
 *  3. sinon l'ID configuré tel quel (préserver la cible de v1.x — ne pas
 *     basculer en silence vers une autre organisation) ;
 *  4. sinon la première organisation de la liste ;
 *  5. sinon `null` (aucun ID configuré et liste vide) → repli de l'appelant.
 *
 * Fonction pure et testable en JVM, isolée de tout état Android.
 */
fun defaultOrganizationId(
    cachedIds: List<String>,
    savedId: String,
    lastUsedId: String?,
): String? = lastUsedId
    ?.takeIf { it in cachedIds }
    ?: savedId.takeIf { it in cachedIds }
    ?: savedId.takeIf { it.isNotBlank() }
    ?: cachedIds.firstOrNull()
