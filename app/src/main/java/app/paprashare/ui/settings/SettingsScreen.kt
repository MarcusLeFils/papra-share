package app.paprashare.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paprashare.R
import app.paprashare.data.PapraSettings
import app.paprashare.ui.theme.PapraTheme

/**
 * Écran de configuration de l'instance Papra, accessible depuis le lanceur.
 *
 * La confirmation d'enregistrement est affichée EN LIGNE dans la zone défilable,
 * au-dessus de la position du clavier virtuel — un snackbar ancré en bas serait
 * masqué par le clavier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
) {
    PapraTheme {
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val saved by viewModel.saved.collectAsStateWithLifecycle()

        Scaffold(
            topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = settings.instanceUrl,
                    onValueChange = { onEdit(viewModel, settings.copy(instanceUrl = it)) },
                    label = { Text(stringResource(R.string.settings_instance_url_label)) },
                    placeholder = { Text(stringResource(R.string.settings_instance_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = settings.apiKey,
                    onValueChange = { onEdit(viewModel, settings.copy(apiKey = it)) },
                    label = { Text(stringResource(R.string.settings_api_key_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = settings.organizationId,
                    onValueChange = { onEdit(viewModel, settings.copy(organizationId = it)) },
                    label = { Text(stringResource(R.string.settings_org_id_label)) },
                    placeholder = { Text(stringResource(R.string.settings_org_id_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.settings_permission_hint),
                    style = MaterialTheme.typography.bodySmall,
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_save))
                }

                if (saved) {
                    Text(
                        text = stringResource(R.string.settings_saved),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(onClick = onBack, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
                    Text(stringResource(R.string.settings_done))
                }
            }
        }
    }
}

/** Met à jour les réglages et efface l'indicateur « enregistré » dès qu'on édite. */
private fun onEdit(viewModel: SettingsViewModel, value: PapraSettings) {
    viewModel.update(value)
    viewModel.resetSaved()
}