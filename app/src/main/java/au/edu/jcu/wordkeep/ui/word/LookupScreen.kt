package au.edu.jcu.wordkeep.ui.word

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.repository.LookupResult
import au.edu.jcu.wordkeep.ui.components.EmptyState
import au.edu.jcu.wordkeep.ui.components.Hairline
import au.edu.jcu.wordkeep.ui.components.PhoneticText
import au.edu.jcu.wordkeep.ui.components.PronounceButton
import au.edu.jcu.wordkeep.ui.components.SenseList

@Composable
fun LookupRoute(
    onBack: () -> Unit,
    onOpenWord: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LookupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    LookupScreen(
        query = viewModel.query,
        state = state,
        playback = playback,
        onBack = onBack,
        onSave = viewModel::save,
        onRetry = viewModel::load,
        onPronounce = viewModel::pronounce,
        onOpenWord = onOpenWord,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupScreen(
    query: String,
    state: LookupUiState,
    playback: PlaybackState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onPronounce: () -> Unit,
    onOpenWord: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dictionary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
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
            when (state) {
                LookupUiState.Loading -> Row(
                    Modifier.fillMaxWidth().padding(48.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
                is LookupUiState.Result -> when (val result = state.result) {
                    is LookupResult.Found -> FoundEntry(result, state, playback, onSave, onPronounce, onOpenWord)
                    is LookupResult.NotFound -> EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = "“${result.text}” isn’t in the dictionary",
                        body = "Check the spelling, or try the base form (for example “analyse” rather than “analysing”).",
                    )
                    LookupResult.Invalid -> EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = "That doesn’t look like a word",
                        body = "Use letters only, for example “hypothesis”.",
                    )
                    is LookupResult.Unavailable -> when (result.reason) {
                        LookupResult.Reason.OFFLINE_MODE -> EmptyState(
                            icon = Icons.Rounded.CloudOff,
                            title = "Offline mode is on",
                            body = "Looking up “$query” would send it to dictionaryapi.dev. Turn offline mode off in Settings if you want to.",
                            actionLabel = "Open settings",
                            onAction = onOpenSettings,
                        )
                        LookupResult.Reason.NO_CONNECTION -> EmptyState(
                            icon = Icons.Rounded.CloudOff,
                            title = "No internet connection",
                            body = "Your saved words still work offline. Try this look-up again when you are connected.",
                            actionLabel = "Try again",
                            onAction = onRetry,
                        )
                        LookupResult.Reason.SERVER_ERROR -> EmptyState(
                            icon = Icons.Rounded.ErrorOutline,
                            title = "The dictionary service didn’t respond",
                            body = "This is usually temporary.",
                            actionLabel = "Try again",
                            onAction = onRetry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoundEntry(
    result: LookupResult.Found,
    state: LookupUiState.Result,
    playback: PlaybackState,
    onSave: () -> Unit,
    onPronounce: () -> Unit,
    onOpenWord: (Long) -> Unit,
) {
    val entry = result.entry
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.text, style = MaterialTheme.typography.displaySmall)
            PhoneticText(entry.phonetic)
        }
        PronounceButton(entry.text, playback, onPronounce)
    }
    Hairline()
    SenseList(entry.senses)
    val savedId = state.savedId
    if (savedId != null) {
        OutlinedButton(onClick = { onOpenWord(savedId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Saved. Open in my words")
        }
    } else {
        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(if (state.saving) "Saving…" else "Save to my words") }
    }
    Text(
        "Definitions: Wiktionary contributors via Free Dictionary API, CC BY-SA 3.0. " +
            "Saving keeps a copy on this device only.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
