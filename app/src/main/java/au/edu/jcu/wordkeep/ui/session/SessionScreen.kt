package au.edu.jcu.wordkeep.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.ui.components.ConfirmDialog
import au.edu.jcu.wordkeep.ui.components.BrandIllustration
import au.edu.jcu.wordkeep.ui.components.EmptyState
import au.edu.jcu.wordkeep.ui.components.Illustration
import au.edu.jcu.wordkeep.ui.components.illustration
import au.edu.jcu.wordkeep.ui.components.isLargeText
import au.edu.jcu.wordkeep.ui.components.plateColor
import au.edu.jcu.wordkeep.ui.friendly
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme

/** Callbacks from the session screen to its view model, grouped to keep signatures short. */
data class SessionActions(
    val onReveal: () -> Unit = {},
    val onGrade: (Grade) -> Unit = {},
    val onChoose: (Int) -> Unit = {},
    val onAttemptChange: (String) -> Unit = {},
    val onSubmitSpelling: () -> Unit = {},
    val onGiveUp: () -> Unit = {},
    val onToggleHint: () -> Unit = {},
    val onPronounce: () -> Unit = {},
    val onNext: () -> Unit = {},
)

@Composable
fun SessionRoute(
    mode: StudyMode,
    onFinished: (StudyMode, SessionSummary) -> Unit,
    onExit: () -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel<SessionViewModel, SessionViewModel.Factory>(
        creationCallback = { factory -> factory.create(mode) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        (state as? SessionUiState.Finished)?.let { onFinished(it.mode, it.summary) }
    }

    SessionScreen(
        state = state,
        playback = playback,
        settings = settings,
        actions = SessionActions(
            onReveal = viewModel::reveal,
            onGrade = viewModel::grade,
            onChoose = viewModel::choose,
            onAttemptChange = viewModel::updateAttempt,
            onSubmitSpelling = viewModel::submitSpelling,
            onGiveUp = viewModel::giveUpSpelling,
            onToggleHint = viewModel::toggleHint,
            onPronounce = viewModel::pronounce,
            onNext = viewModel::next,
        ),
        onExit = {
            viewModel.stopAudio()
            onExit()
        },
        onOpenLibrary = onOpenLibrary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    state: SessionUiState,
    playback: PlaybackState,
    settings: UserSettings,
    actions: SessionActions,
    onExit: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val active = state as? SessionUiState.Active
    // Leaving mid-session asks first; answers already given are kept either way.
    BackHandler(enabled = active != null) { confirmExit = true }

    Scaffold(
        topBar = {
            // A header band in the activity's plate colour, with its illustration at the end: the
            // same pairing as the activity's tile on Today. The question area below stays plain.
            Column(Modifier.background(state.mode.plateColor())) {
                TopAppBar(
                    title = {
                        Column {
                            Text(state.mode.title, style = MaterialTheme.typography.titleMedium)
                            if (active != null) {
                                Text(
                                    "${active.position + 1} of ${active.total}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (active != null) confirmExit = true else onExit() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "End session")
                        }
                    },
                    actions = {
                        if (!isLargeText()) {
                            BrandIllustration(
                                state.mode.illustration,
                                Modifier
                                    .padding(end = 12.dp)
                                    .size(56.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
                if (active != null) {
                    LinearProgressIndicator(
                        progress = { active.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                            .semantics { contentDescription = "Question ${active.position + 1} of ${active.total}" },
                        trackColor = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (state) {
                    is SessionUiState.Loading, is SessionUiState.Finished ->
                        CircularProgressIndicator(Modifier.padding(48.dp))
                    is SessionUiState.Empty -> EmptySession(state, onExit, onOpenLibrary)
                    is SessionUiState.Active -> ActiveItem(state, playback, settings, actions)
                }
            }
        }
    }

    if (confirmExit) {
        ConfirmDialog(
            title = "End this session?",
            body = "Answers you have already given are saved. The rest stay for next time.",
            confirmLabel = "End session",
            destructive = false,
            onConfirm = {
                confirmExit = false
                onExit()
            },
            onDismiss = { confirmExit = false },
        )
    }
}

@Composable
private fun ActiveItem(
    state: SessionUiState.Active,
    playback: PlaybackState,
    settings: UserSettings,
    actions: SessionActions,
) {
    val reduceMotion = WordkeepTheme.accessibility.reduceMotion
    // Keyed by position: answering within an item updates in place; moving on slides to the next.
    AnimatedContent(
        targetState = state,
        contentKey = { it.position },
        transitionSpec = {
            if (reduceMotion) {
                fadeIn() togetherWith fadeOut()
            } else {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            }
        },
        label = "sessionItem",
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
    ) { shown ->
        when (val item = shown.item) {
            is SessionItem.Flashcard -> FlashcardContent(item, playback, actions)
            is SessionItem.Meaning -> MeaningContent(item, playback, actions)
            is SessionItem.Spelling -> SpellingContent(item, playback, settings.spellingHints, actions)
        }
    }
}

@Composable
private fun EmptySession(state: SessionUiState.Empty, onExit: () -> Unit, onOpenLibrary: () -> Unit) {
    Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.Center) {
        when (state.reason) {
            EmptyReason.NOTHING_DUE -> EmptyState(
                art = Illustration.DayComplete,
                title = "Nothing due right now",
                body = state.nextReview?.let { "Your next review is ${it.friendly(state.today)}. Spacing reviews out is what makes words stick." }
                    ?: "You have reached today’s limit for new words.",
                actionLabel = "Back to Today",
                onAction = onExit,
            )
            EmptyReason.NOT_ENOUGH_WORDS -> EmptyState(
                art = Illustration.EmptyLibrary,
                title = "A few more words needed",
                body = "This activity needs at least three words with definitions to build fair questions.",
                actionLabel = "Add words",
                onAction = onOpenLibrary,
            )
            EmptyReason.NO_WORDS -> EmptyState(
                art = Illustration.EmptyLibrary,
                title = "No words yet",
                body = "Look up a word or add the starter list to begin.",
                actionLabel = "Add words",
                onAction = onOpenLibrary,
            )
        }
    }
}
