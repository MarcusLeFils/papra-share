package app.paprashare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import app.paprashare.ui.share.ShareScreen
import app.paprashare.ui.share.ShareViewModel
import app.paprashare.util.ShareIntentResolver

/**
 * Activité de partage : reçoit ACTION_SEND / ACTION_SEND_MULTIPLE de
 * documents et scans, puis les upload vers l'instance Papra configurée.
 */
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Résout la liste de documents reçue avant la première composition.
        val documents = ShareIntentResolver.resolve(intent, contentResolver)
            ?: run {
                finish()
                return
            }

        setContent {
            ShareScreen(
                viewModel = viewModel,
                onClose = { finish() },
                onOpenSettings = {
                    startActivity(
                        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    finish()
                },
            )
        }

        viewModel.init(documents)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Pour singleTop : re-résoudre et relancer l'init si besoin.
        val docs = ShareIntentResolver.resolve(intent, contentResolver) ?: return
        viewModel.init(docs)
    }
}