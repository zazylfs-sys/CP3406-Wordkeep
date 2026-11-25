package au.edu.jcu.wordkeep.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.ui.components.ConfirmDialog
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.SwitchRow
import au.edu.jcu.wordkeep.ui.plural
import java.time.LocalDate

@Composable
fun YourDataRoute(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: YourDataViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::export)
    }
    LaunchedEffect(message) {
        when (val current = message) {
            is DataMessage.Exported -> snackbar.showSnackbar("Exported ${plural(current.words, "word")}.")
            DataMessage.ExportFailed -> snackbar.showSnackbar("Export failed. Nothing was written.")
            DataMessage.Deleted -> onDeleted()
            null -> return@LaunchedEffect
        }
        viewModel.messageShown()
    }
    YourDataScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onExport = { exportLauncher.launch("wordkeep-${LocalDate.now()}.csv") },
        onDeleteEverything = viewModel::deleteEverything,
        onOfflineModeChange = viewModel::setOfflineMode,
    )
}

/** Plain-language account of the app's data practices, with export and erase controls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourDataScreen(
    state: YourDataUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onDeleteEverything: () -> Unit,
    onOfflineModeChange: (Boolean) -> Unit = {},
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your data") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Fact(
                icon = Icons.Rounded.PhoneAndroid,
                title = "Stored only on this device",
                body = "${plural(state.words, "word")}, ${plural(state.answers, "answer")} and your settings. " +
                    "Cloud backup is switched off for this app, so none of it is copied to your Google account.",
            )
            Fact(
                icon = Icons.Rounded.CloudUpload,
                title = if (state.offlineMode) "Sent over the internet: nothing (offline mode is on)" else "Sent over the internet",
                body = "When you look up a word (or download the starter list), only the word is sent to dictionaryapi.dev. " +
                    "For quiz options, the word being studied is sent to api.datamuse.com. " +
                    "Pronunciation recordings are streamed from dictionaryapi.dev. No account, device ID or location is attached; " +
                    "like any website, these services can see your network’s IP address.",
            )
            SwitchRow(
                title = "Offline mode",
                description = "Stop all network use. Saved words and reviews keep working.",
                checked = state.offlineMode,
                onCheckedChange = onOfflineModeChange,
            )
            Fact(
                icon = Icons.Rounded.Block,
                title = "Never collected",
                body = "Location, contacts, microphone, advertising ID, analytics or crash reports. " +
                    "The app asks for one permission only: internet access.",
            )

            PaperCard(Modifier.fillMaxWidth()) {
                SectionLabel("Take a copy")
                Text(
                    "Export your words, definitions and progress as a CSV file you can open in a spreadsheet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                OutlinedButton(onClick = onExport, modifier = Modifier.heightIn(min = 48.dp)) { Text("Export as CSV") }
            }
            PaperCard(Modifier.fillMaxWidth()) {
                SectionLabel("Erase")
                Text(
                    "Delete every word, answer and setting. Wordkeep returns to its first-run screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Delete everything") }
            }

            SectionLabel("Sources and licences")
            Text(
                "Definitions and recordings: Wiktionary contributors via Free Dictionary API (dictionaryapi.dev), CC BY-SA 3.0. " +
                    "Look-alike words: Datamuse API. Starter list: Academic Word List sublist 1, A. Coxhead (2000). " +
                    "Fonts: Literata and IBM Plex Sans, SIL Open Font License 1.1. Scheduling: SM-2 algorithm, P. Wozniak (1990).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete everything?",
            body = "This removes ${plural(state.words, "word")} and ${plural(state.answers, "answer")} from this device. It cannot be undone. Export a copy first if you want one.",
            confirmLabel = "Delete everything",
            onConfirm = {
                confirmDelete = false
                onDeleteEverything()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Fact(icon: ImageVector, title: String, body: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
