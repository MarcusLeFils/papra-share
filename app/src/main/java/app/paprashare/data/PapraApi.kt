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

/**
 * Catégorie d'un échec d'upload, indépendante de la locale.
 * La couche UI la mappe vers une ressource de chaîne (`R.string`).
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
                        kind = errorKind(response.code),
                        details = fullError(response.code, response.message, body),
                    )
                } else {
                    val documentId = extractDocumentId(body)
                    if (documentId == null) {
                        // fail loud : réponse 2xx mais id introuvable → contrat non respecté.
                        UploadResult.Failure(
                            kind = PapraErrorKind.INVALID_RESPONSE,
                            details = fullError(-1, "no document id", body),
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
                kind = PapraErrorKind.NETWORK,
                details = e.message ?: e.javaClass.simpleName,
            )
        } finally {
            runCatching { data.close() }
        }
    }

    /** Catégorie d'erreur à partir du code HTTP (cartographiée vers une ressource côté UI). */
    private fun errorKind(code: Int): PapraErrorKind = when (code) {
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

    /** Détails bruts utiles au bouton « Détails » (non localisés). */
    private fun fullError(code: Int, status: String, body: String): String {
        val parts = mutableListOf<String>()
        if (code > 0) parts += "HTTP $code"
        if (status.isNotBlank()) parts += status
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