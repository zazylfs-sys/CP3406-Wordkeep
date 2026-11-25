package au.edu.jcu.wordkeep.ui.word

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.ui.components.ConfirmDialog
import au.edu.jcu.wordkeep.ui.components.EmptyState
import au.edu.jcu.wordkeep.ui.components.Hairline
import au.edu.jcu.wordkeep.ui.components.MasteryTag
import au.edu.jcu.wordkeep.ui.components.NoticeBanner
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.PhoneticText
import au.edu.jcu.wordkeep.ui.components.PronounceButton
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.SenseList
import au.edu.jcu.wordkeep.ui.friendly
import au.edu.jcu.wordkeep.ui.plural
import java.time.LocalDate

@Composable
fun WordDetailRoute(onBack: () -> Unit, viewModel: WordDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) { if (deleted) onBack() }
    WordDetailScreen(
        state = state,
        playback = playback,
        onBack = onBack,
        onPronounce = viewModel::pronounce,
        onSaveNote = viewModel::saveNote,
        onResetProgress = viewModel::resetProgress,
        onDelete = viewModel::delete,
        onRetryDownload = viewModel::retryDownload,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    state: WordDetailUiState,
    playback: PlaybackState,
    onBack: () -> Unit,
    onPronounce: () -> Unit,
    onSaveNote: (String) -> Unit,
    onResetProgress: () -> Unit,
    onDelete: () -> Unit,
    onRetryDownload: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (state is WordDetailUiState.Ready) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete word")
                        }
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (state) {
                WordDetailUiState.Loading -> CircularProgressIndicator(Modifier.padding(48.dp))
                WordDetailUiState.Missing -> EmptyState(Icons.Rounded.SearchOff, "Word not found", "It may have been deleted.")
                is WordDetailUiState.Ready -> WordDetailContent(
                    state.word, state.today, playback, onPronounce, onSaveNote, onRetryDownload,
                    onResetClick = { confirmReset = true },
                )
            }
        }
    }

    val word = (state as? WordDetailUiState.Ready)?.word?.word?.text
    if (confirmDelete && word != null) {
        ConfirmDialog(
            title = "Delete “$word”?",
            body = "The word, its definitions and every answer you gave for it are removed from this device. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmReset && word != null) {
        ConfirmDialog(
            title = "Reset progress for “$word”?",
            body = "It becomes a new word again. Your past answers stay in your statistics.",
            confirmLabel = "Reset",
            onConfirm = {
                confirmReset = false
                onResetProgress()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun WordDetailContent(
    studyWord: StudyWord,
    today: LocalDate,
    playback: PlaybackState,
    onPronounce: () -> Unit,
    onSaveNote: (String) -> Unit,
    onRetryDownload: () -> Unit,
    onResetClick: () -> Unit,
) {
    val (word, review) = studyWord
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(word.text, style = MaterialTheme.typography.displaySmall)
            PhoneticText(word.phonetic)
        }
        if (word.isReady) PronounceButton(word.text, playback, onPronounce)
    }
    when (word.fetchState) {
        FetchState.PENDING -> NoticeBanner(Icons.Rounded.CloudDownload, "Definition not downloaded yet.", actionLabel = "Download", onAction = onRetryDownload)
        FetchState.NOT_FOUND -> NoticeBanner(Icons.Rounded.SearchOff, "The dictionary has no entry for this word.")
        FetchState.READY -> Unit
    }
    Hairline()
    SenseList(word.senses)

    NoteEditor(initial = word.note.orEmpty(), onSave = onSaveNote)

    if (word.isReady) {
        PaperCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Your progress", Modifier.weight(1f))
                MasteryTag(review.mastery)
            }
            Spacer(Modifier.heightIn(min = 8.dp))
            ProgressLine("Next review", review.dueDate?.let { if (it.isBefore(today)) "overdue" else it.friendly(today) } ?: "not studied yet")
            ProgressLine("Current interval", if (review.isNew) "—" else plural(review.intervalDays, "day"))
            ProgressLine("Times forgotten", review.lapses.toString())
            ProgressLine("Ease", "%.2f".format(review.easeFactor))
            if (!review.isNew) {
                TextButton(onClick = onResetClick) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset progress")
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

/** The learner's own memory hook. Self-generated cues are a well-known aid to recall. */
@Composable
private fun NoteEditor(initial: String, onSave: (String) -> Unit) {
    var draft by rememberSaveable(initial) { mutableStateOf(initial) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Memory hook")
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(200) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("A sentence or mental picture that helps you remember") },
            supportingText = { Text("Shown on the back of the flashcard. Stays on this device.") },
            minLines = 2,
        )
        if (draft != initial) {
            OutlinedButton(onClick = { onSave(draft) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Save hook") }
        }
    }
}
