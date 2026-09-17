package app.paprashare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import app.paprashare.ui.settings.SettingsScreen
import app.paprashare.ui.settings.SettingsViewModel

/**
 * Activité principale : configuration de l'instance Papra
 * (URL, clé API, ID d'organisation). Accessible depuis le lanceur.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(viewModel = viewModel, onBack = { finish() })
        }
    }
}