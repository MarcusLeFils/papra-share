package app.paprashare.ui.share

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paprashare.R
import app.paprashare.ui.theme.PapraTheme
import app.paprashare.util.SharedDocument
import kotlinx.coroutines.flow.MutableStateFlow

/** Écran exécuté au sein de l'activité de partage reçue depuis le système. */
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showDetails = remember { MutableStateFlow(false) }
    val showDetailsValue by showDetails.collectAsStateWithLifecycle()

    PapraTheme {
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.share_close))
                }
                when (state) {
                    is ShareUiState.Loading -> {
                        CircularProgressIndicator()
                    }

                    is ShareUiState.Ready -> ShareContent(
                        viewModel = viewModel,
                        isConfigured = (state as ShareUiState.Ready).isConfigured,
                        onOpenSettings = onOpenSettings,
                    )

                    is ShareUiState.Uploading -> {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.share_uploading),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = viewModel.documents.joinToString(", ") { it.displayName },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                        )
                    }

                    is ShareUiState.Success -> {
                        val success = state as ShareUiState.Success
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = pluralStringResource(R.plurals.share_success, success.count, success.count),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.share_close))
                        }
                    }

                    is ShareUiState.Error -> {
                        val error = state as ShareUiState.Error
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(error.messageRes, *error.messageArgs.toTypedArray()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        )
                        if (!error.details.isNullOrEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { showDetails.value = !showDetails.value },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (showDetailsValue) {
                                        stringResource(R.string.share_hide_details)
                                    } else {
                                        stringResource(R.string.share_show_details)
                                    },
                                )
                            }
                            if (showDetailsValue) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = error.details.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .verticalScroll(rememberScrollState()),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.share_close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareContent(
    viewModel: ShareViewModel,
    isConfigured: Boolean,
    onOpenSettings: () -> Unit,
) {
    Header(viewModel.documents)
    Spacer(Modifier.height(20.dp))
    if (isConfigured) {
        Button(
            onClick = { viewModel.upload() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.share_title))
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.share_not_configured),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.share_not_configured_action))
            }
        }
    }
}

@Composable
private fun Header(documents: List<SharedDocument>) {
    Text(
        text = stringResource(R.string.share_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            documents.forEach { doc ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(doc.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(doc.mimeType, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}