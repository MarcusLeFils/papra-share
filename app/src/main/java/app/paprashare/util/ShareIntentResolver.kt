package app.paprashare.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

/** Document reçu depuis une intention de partage. */
data class SharedDocument(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
)

object ShareIntentResolver {

    /**
     * Lit une intention [Intent] partagée et restitue la liste des documents.
     *
     * - clipData : liste des éléments partagés (présente pour SEND comme SEND_MULTIPLE).
     * - EXTRA_STREAM : liste (SEND_MULTIPLE) ou unique (SEND).
     *
     * Le même document apparaît souvent dans **clipData ET EXTRA_STREAM** : on déduplique
     * par URI pour éviter d'envoyer deux fois le même fichier.
     *
     * Retourne null si l'intention ne contient aucun URI exploitable.
     */
    fun resolve(intent: Intent, resolver: ContentResolver): List<SharedDocument>? {
        val clip = intent.clipData?.let { clipData ->
            (0 until clipData.itemCount).mapNotNull { i -> clipData.getItemAt(i)?.uri }
        } ?: emptyList()

        val extra = if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
        } else {
            listOfNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        }

        val streams = (clip + extra)
            .filter { it.scheme == "content" || it.scheme == "file" }
            .distinctBy { it.toString() }

        if (streams.isEmpty()) return null

        return streams.map { uri ->
            val mime = mimeType(uri, resolver)
            SharedDocument(
                uri = uri,
                displayName = displayName(uri, resolver, mime),
                mimeType = mime,
            )
        }
    }

    /** Nom de fichier auprès du fournisseur de contenu, avec repli. */
    private fun displayName(uri: Uri, resolver: ContentResolver, mime: String): String {
        val fromProvider = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                }
        }.getOrNull()

        return fromProvider ?: genericName(uri, mime)
    }

    private fun genericName(uri: Uri, mime: String): String {
        val last = uri.lastPathSegment ?: "document"
        if (last.contains('.')) return last
        val ext = mime.substringAfter('/').takeIf { it != "octet-stream" }
        return if (ext.isNullOrEmpty()) last else "$last.$ext"
    }

    private fun mimeType(uri: Uri, resolver: ContentResolver): String =
        runCatching { resolver.getType(uri) }
            .getOrNull()
            ?: "application/octet-stream"
}