package app.paprashare.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Organisation Papra, telle que renvoyée par `GET /api/organizations`.
 *
 * @property id   identifiant stable, porté dans l'URL d'upload
 * @property name nom affichable à l'utilisateur (libellé du sélecteur)
 */
@Serializable
data class Organization(
    val id: String,
    val name: String,
)

/** Enveloppe de réponse de la liste des organisations. */
@Serializable
private data class OrganizationsEnvelope(
    val organizations: List<Organization> = emptyList(),
)

/**
 * Instance Json partagée. `ignoreUnknownKeys` : l'API Papra renvoie des champs
 * supplémentaires (plan, membersCount…) que ce client ne consomme pas ; on les
 * ignore au lieu de casser le décodage.
 */
private val papraJson: Json = Json { ignoreUnknownKeys = true }

/**
 * Décode la réponse de `GET /api/organizations`.
 *
 * Toute entrée invalide (pas du JSON, réponse sans clé `organizations`) renvoie
 * une liste vide — jamais d'exception. C'est le contrat de repli propre de
 * l'écran de partage : la liste est facultative, l'envoi vers l'ID configuré
 * doit toujours survivre.
 */
fun parseOrganizations(json: String): List<Organization> = runCatching {
    papraJson.decodeFromString<OrganizationsEnvelope>(json).organizations
}.getOrDefault(emptyList())

/** Sérialise la liste pour le cache DataStore (JSON brut, relisible par [parseOrganizations]). */
fun serializeOrganizations(organizations: List<Organization>): String =
    papraJson.encodeToString(OrganizationsEnvelope(organizations))