package app.paprashare.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.paprashare.domain.normalizeInstanceUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Paramètres persistés de l'application : instance Papra, clé API, organisation. */
data class PapraSettings(
    val instanceUrl: String = "",
    val apiKey: String = "",
    val organizationId: String = "",
) {
    val isConfigured: Boolean
        get() = instanceUrl.isNotBlank() &&
            apiKey.isNotBlank() &&
            organizationId.isNotBlank()
}

private val Context.dataStore by preferencesDataStore(name = "papra_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val INSTANCE_URL = stringPreferencesKey("instance_url")
        val API_KEY = stringPreferencesKey("api_key")
        val ORGANIZATION_ID = stringPreferencesKey("organization_id")
    }

    val settings: Flow<PapraSettings> = context.dataStore.data.map { prefs ->
        PapraSettings(
            instanceUrl = prefs[Keys.INSTANCE_URL].orEmpty(),
            apiKey = prefs[Keys.API_KEY].orEmpty(),
            organizationId = prefs[Keys.ORGANIZATION_ID].orEmpty(),
        )
    }

    suspend fun save(settings: PapraSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INSTANCE_URL] = normalizeInstanceUrl(settings.instanceUrl)
            prefs[Keys.API_KEY] = settings.apiKey.trim()
            prefs[Keys.ORGANIZATION_ID] = settings.organizationId.trim()
        }
    }
}
