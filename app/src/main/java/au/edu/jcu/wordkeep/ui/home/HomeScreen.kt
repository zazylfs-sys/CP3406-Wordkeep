package au.edu.jcu.wordkeep.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.data.repository.LookupResult
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.ui.components.Hairline
import au.edu.jcu.wordkeep.ui.components.NoticeBanner
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.StatFigure
import au.edu.jcu.wordkeep.ui.friendly
import au.edu.jcu.wordkeep.ui.longDate
import au.edu.jcu.wordkeep.ui.plural

@Composable
fun HomeRoute(
    onStartSession: (StudyMode) -> Unit,
    onLookUp: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    wideLayout: Boolean,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDate() }
    HomeScreen(
        state = state,
        wideLayout = wideLayout,
        onStartSession = onStartSession,
        onLookUp = onLookUp,
        onOpenLibrary = onOpenLibrary,
        onRetryDownload = viewModel::downloadPending,
    )
}

/** The landing page: what is due today, the three activities, and a dictionary search. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    wideLayout: Boolean,
    onStartSession: (StudyMode) -> Unit,
    onLookUp: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onRetryDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Masthead(state)
        Notices(state, onRetryDownload, onOpenLibrary)
        if (wideLayout) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    TodayDesk(state, onStartSession, onOpenLibrary)
                    LookUpField(onLookUp)
                }
                Activities(state, onStartSession, Modifier.weight(1f))
            }
        } else {
            TodayDesk(state, onStartSession, onOpenLibrary)
            Activities(state, onStartSession)
            LookUpField(onLookUp)
        }
        PrivacySeal()
    }
}

@Composable
private fun Masthead(state: HomeUiState) {
    Column {
        Text(
            "Wordkeep",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        if (!state.loading) {
            Text(
                state.today.longDate(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Notices(state: HomeUiState, onRetryDownload: () -> Unit, onOpenLibrary: () -> Unit) {
    when {
        state.offlineMode -> NoticeBanner(
            icon = Icons.Rounded.CloudOff,
            text = "Offline mode is on. Nothing leaves this device; new look-ups are paused.",
        )
        state.download is DownloadUi.Running -> {
            val running = state.download
            val text = if (running.total == 0) "Preparing definitions…"
            else "Downloading definitions: ${running.done} of ${running.total}"
            NoticeBanner(icon = Icons.Rounded.CloudDownload, text = text)
        }
        state.download is DownloadUi.Stopped -> NoticeBanner(
            icon = Icons.Rounded.CloudOff,
            text = when (state.download.reason) {
                LookupResult.Reason.SERVER_ERROR -> "The dictionary service is not responding. ${plural(state.pendingDownloads, "word")} still to download."
                else -> "No connection. ${plural(state.pendingDownloads, "word")} still need definitions."
            },
            actionLabel = "Retry",
            onAction = onRetryDownload,
        )
        state.pendingDownloads > 0 -> NoticeBanner(
            icon = Icons.Rounded.CloudDownload,
            text = "${plural(state.pendingDownloads, "word")} still need definitions.",
            actionLabel = "Download",
            onAction = onRetryDownload,
        )
        !state.loading && state.totalWords == 0 -> NoticeBanner(
            icon = Icons.Rounded.Search,
            text = "Your word list is empty. Look up a word to start.",
            actionLabel = "Words",
            onAction = onOpenLibrary,
        )
    }
}

@Composable
private fun TodayDesk(state: HomeUiState, onStartSession: (StudyMode) -> Unit, onOpenLibrary: () -> Unit) {
    PaperCard(Modifier.fillMaxWidth()) {
        SectionLabel("Today’s desk")
        Spacer(Modifier.height(12.dp))
        // FlowRow: with very large text the figures wrap as whole units instead of squeezing.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatFigure(state.dueCount.toString(), "due for review")
            StatFigure(state.newAvailable.toString(), "new words")
            StatFigure(state.answeredToday.toString(), "answered today")
        }
        Spacer(Modifier.height(16.dp))
        if (state.hasSessionWork) {
            Button(
                onClick = { onStartSession(StudyMode.FLASHCARDS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text("Start today’s review")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "About ${estimatedMinutes(state)} min. The session ends when today’s cards are done.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (!state.loading) {
            val next = state.nextReview?.let { "Next review: ${it.friendly(state.today)}." } ?: ""
            Text(
                if (state.readyWords == 0) "Add some words to begin." else "You’re up to date. $next",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (state.readyWords == 0) "The library has a starter list and a dictionary search."
                else "Rest is part of remembering. You can still practise with a quiz below; it won’t change your schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.readyWords == 0) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenLibrary) { Text("Open words") }
            }
        }
    }
}

private fun estimatedMinutes(state: HomeUiState): Int =
    ((state.dueCount + state.newAvailable) * 12 / 60).coerceAtLeast(1)

@Composable
private fun Activities(state: HomeUiState, onStartSession: (StudyMode) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Practise")
        ActivityRow(
            icon = Icons.Rounded.Style,
            title = StudyMode.FLASHCARDS.title,
            body = "Recall the meaning, then rate how well you knew it.",
            enabled = state.hasSessionWork,
            disabledReason = "Nothing due right now",
            onClick = { onStartSession(StudyMode.FLASHCARDS) },
        )
        ActivityRow(
            icon = Icons.Rounded.Quiz,
            title = StudyMode.MEANING_MATCH.title,
            body = "Pick the word that fits a definition. Look-alike words keep it honest.",
            enabled = state.canQuiz,
            disabledReason = "Needs at least 3 words with definitions",
            onClick = { onStartSession(StudyMode.MEANING_MATCH) },
        )
        ActivityRow(
            icon = Icons.Rounded.Hearing,
            title = StudyMode.LISTEN_AND_SPELL.title,
            body = "Hear the word, then type it. Close attempts get a second try.",
            enabled = state.canSpell,
            disabledReason = "Needs a word with a definition",
            onClick = { onStartSession(StudyMode.LISTEN_AND_SPELL) },
        )
    }
}

@Composable
private fun ActivityRow(
    icon: ImageVector,
    title: String,
    body: String,
    enabled: Boolean,
    disabledReason: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (enabled) body else disabledReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LookUpField(onLookUp: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Dictionary")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            label = { Text("Look up a word") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onLookUp(query.trim()) }),
        )
    }
}

/** A quiet reminder of the app's data promise, at the foot of the landing page. */
@Composable
private fun PrivacySeal() {
    Column {
        Hairline()
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Stored on this device · No account · No tracking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
