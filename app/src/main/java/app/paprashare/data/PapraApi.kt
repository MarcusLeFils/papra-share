package app.paprashare.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** Résultat d'un upload. */
sealed class UploadResult {
    data class Success(val documentId: String) : UploadResult()
    data class Failure(
        val message: String,
        val details: String? = null,
    ) : UploadResult()
}

/**
 * Client minimal de l'API Papra.
 *
 * Endpoint d'upload — POST {instanceUrl}/api/organizations/{orgId}/documents
 * en multipart form-data, champ "file", en-tête `Authorization: Bearer <apiKey>`.
 * Permission requise sur la clé : `documents:create`.
 */
class PapraApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Upload d'un flux binaire vers Papra.
     *
     * @param settings configuration (URL, clé, organisation)
     * @param fileName nom de fichier
     * @param mimeType type MIME du document
     * @param data     flux lisible du contenu (toujours fermé ici, succès ou échec)
     */
    suspend fun uploadDocument(
        settings: PapraSettings,
        fileName: String,
        mimeType: String,
        data: InputStream,
    ): UploadResult = withContext(Dispatchers.IO) {
        // La fermeture du flux doit être garantie même si execute() échoue avant
        // l'écriture du body (sinon fuite de descripteur). try/finally englobant.
        try {
            val base = settings.instanceUrl.trim().trimEnd('/')
            val url = "$base/api/organizations/${settings.organizationId.trim()}/documents"

            val mediaType = mimeType.toMediaTypeOrNull()
                ?: "application/octet-stream".toMediaType()

            val filePart = object : RequestBody() {
                override fun contentType() = mediaType
                // Flux de taille inconnue → -1 ; OkHttp bascule en chunked.
                override fun contentLength(): Long = -1L
                override fun isOneShot() = true
                override fun writeTo(sink: BufferedSink) {
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    while (data.read(buffer).also { read = it } != -1) {
                        if (read > 0) sink.write(buffer, 0, read)
                    }
                }
            }

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, filePart)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${settings.apiKey.trim()}")
                .post(multipart)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    UploadResult.Failure(
                        message = friendlyError(response.code),
                        details = fullError("HTTP ${response.code}", response.message, body),
                    )
                } else {
                    val documentId = extractDocumentId(body)
                    if (documentId == null) {
                        // fail loud : réponse 2xx mais id introuvable → contrat non respecté.
                        UploadResult.Failure(
                            message = "Le serveur n'a pas renvoyé d'identifiant de document.",
                            details = fullError(null, "Réponse inattendue", body),
                        )
                    } else {
                        UploadResult.Success(documentId)
                    }
                }
            }
        } catch (e: CancellationException) {
            // La cancellation (fermeture d'activité, sortie d'écran) DOIT se propager,
            // pas être transformée en échec : sinon ruptures de structured concurrency.
            throw e
        } catch (e: Exception) {
            UploadResult.Failure(
                message = "Impossible de joindre l'instance Papra.",
                details = e.message ?: e.javaClass.simpleName,
            )
        } finally {
            runCatching { data.close() }
        }
    }

    /** Message lisible d'après le code HTTP, pour les erreurs fréquentes. */
    private fun friendlyError(code: Int): String = when (code) {
        400 -> "Requête invalide envoyée au serveur."
        401 -> "Authentification refusée : vérifiez la clé API."
        403 -> "Accès refusé : la clé API doit avoir la permission documents:create."
        404 -> "Ressource introuvable : vérifiez l'URL de l'instance et l'ID d'organisation."
        409 -> "Document en double : un document identique existe déjà."
        413 -> "Fichier trop volumineux pour l'instance Papra."
        429 -> "Trop de requêtes : attendez un instant puis réessayez."
        in 500..599 -> "Erreur interne du serveur Papra."
        else -> "Échec de l'envoi (HTTP $code)."
    }

    /** Détails bruts utiles au bouton « Détails ». */
    private fun fullError(prefix: String?, status: String?, body: String): String {
        val parts = mutableListOf<String>()
        prefix?.let { parts += it }
        status?.let { if (it.isNotBlank()) parts += it }
        val trimmed = body.trim()
        if (trimmed.isNotBlank()) parts += trimmed.take(500)
        return parts.joinToString(" — ")
    }

    /** Extrait l'id du document dans { "document": { ..., "id": "..." } }. */
    private fun extractDocumentId(json: String): String? {
        // Cherche l'id sous la clé "document" d'abord (robustesse à l'ordre des champs).
        val docStart = json.indexOf("\"document\"")
        val scope = if (docStart >= 0) json.substring(docStart) else json
        val match = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(scope)
        return match?.groupValues?.get(1)
            ?: Regex("\"uuid\"\\s*:\\s*\"([^\"]+)\"").find(scope)?.groupValues?.get(1)
    }

    private fun String.toMediaTypeOrNull(): okhttp3.MediaType? =
        runCatching { toMediaType() }.getOrNull()
}