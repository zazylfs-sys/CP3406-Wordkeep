package au.edu.jcu.wordkeep.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.data.repository.LookupResult
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.ui.components.BrandIllustration
import au.edu.jcu.wordkeep.ui.components.Hairline
import au.edu.jcu.wordkeep.ui.components.Illustration
import au.edu.jcu.wordkeep.ui.components.NoticeBanner
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.StatFigure
import au.edu.jcu.wordkeep.ui.friendly
import au.edu.jcu.wordkeep.ui.longDate
import au.edu.jcu.wordkeep.ui.plural
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme

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
        Notices(state, onRetryDownload)
        if (wideLayout) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    TodayHero(state, onStartSession, onOpenLibrary)
                    LookUpField(onLookUp)
                }
                PractiseBento(state, onStartSession, Modifier.weight(1f))
            }
        } else {
            TodayHero(state, onStartSession, onOpenLibrary)
            LookUpField(onLookUp)
            PractiseBento(state, onStartSession)
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
private fun Notices(state: HomeUiState, onRetryDownload: () -> Unit) {
    when {
        state.offlineMode -> OfflineNotice()
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
        // An empty word list is explained by the hero card itself, so it needs no banner.
    }
}

/** Offline mode is a deliberate, calm state, so it gets a small illustration rather than a warning icon. */
@Composable
private fun OfflineNotice() {
    Surface(
        color = WordkeepTheme.colors.plateOchre,
        shape = MaterialTheme.shapes.large,
        border = plateBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandIllustration(Illustration.Offline, Modifier.size(72.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Offline mode is on.", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Nothing leaves this device; new look-ups are paused.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The hero: today's numbers, the main action and an illustration that changes with the day's state
 * (work waiting, up to date, or no words yet). The illustration sits beside the text, never behind it.
 */
@Composable
private fun TodayHero(state: HomeUiState, onStartSession: (StudyMode) -> Unit, onOpenLibrary: () -> Unit) {
    val art = when {
        state.loading || state.hasSessionWork -> Illustration.TodayDesk
        state.readyWords == 0 -> Illustration.EmptyLibrary
        else -> Illustration.DayComplete
    }
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    Surface(
        color = WordkeepTheme.colors.plateHero,
        shape = RoundedCornerShape(24.dp),
        border = plateBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Today’s desk")
                    AccentRule(Modifier.padding(top = 8.dp, bottom = 12.dp))
                    HeroHeadline(state)
                }
                if (!largeText) {
                    BrandIllustration(
                        art,
                        Modifier
                            .padding(start = 8.dp)
                            .size(148.dp)
                            .offset(x = 12.dp, y = (-8).dp),
                    )
                }
            }
            if (!state.loading && state.readyWords > 0) {
                Spacer(Modifier.height(16.dp))
                TodayFigures(state)
            }
            Spacer(Modifier.height(16.dp))
            HeroAction(state, onStartSession, onOpenLibrary)
        }
    }
}

@Composable
private fun HeroHeadline(state: HomeUiState) {
    when {
        state.loading -> Text(" ", style = MaterialTheme.typography.displaySmall)
        state.hasSessionWork -> {
            val total = state.dueCount + state.newAvailable
            Text(
                total.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (total == 1) "word waiting on your desk" else "words waiting on your desk",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        state.readyWords == 0 -> {
            Text("Add some words to begin.", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "The library has a starter list and a dictionary search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            Text("You’re up to date.", style = MaterialTheme.typography.headlineSmall)
            state.nextReview?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Next review: ${it.friendly(state.today)}.",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Rest is part of remembering. You can still practise with a quiz below; it won’t change your schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The three day figures on a lighter band, split by hairlines. FlowRow lets them wrap with very large text. */
@Composable
private fun TodayFigures(state: HomeUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FlowRow(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatFigure(state.dueCount.toString(), "due for review")
            StatFigure(state.newAvailable.toString(), "new words")
            StatFigure(state.answeredToday.toString(), "answered today")
        }
    }
}

@Composable
private fun HeroAction(state: HomeUiState, onStartSession: (StudyMode) -> Unit, onOpenLibrary: () -> Unit) {
    when {
        state.hasSessionWork -> {
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
        }
        !state.loading && state.readyWords == 0 -> Button(
            onClick = onOpenLibrary,
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Open words") }
    }
}

private fun estimatedMinutes(state: HomeUiState): Int =
    ((state.dueCount + state.newAvailable) * 12 / 60).coerceAtLeast(1)

/**
 * The three activities as a bento grid: flashcards as a wide tile, the two quizzes as a pair below.
 * With very large text or a narrow column the pair stacks, so nothing is squeezed.
 */
@Composable
private fun PractiseBento(state: HomeUiState, onStartSession: (StudyMode) -> Unit, modifier: Modifier = Modifier) {
    val colors = WordkeepTheme.colors
    val flashcards = Activity(
        art = Illustration.Flashcards,
        title = StudyMode.FLASHCARDS.title,
        body = "Recall the meaning, then rate how well you knew it.",
        enabled = state.hasSessionWork,
        disabledReason = "Nothing due right now",
        plate = colors.plateCream,
        onClick = { onStartSession(StudyMode.FLASHCARDS) },
    )
    val meaning = Activity(
        art = Illustration.MeaningMatch,
        title = StudyMode.MEANING_MATCH.title,
        body = "Pick the word that fits a definition.",
        enabled = state.canQuiz,
        disabledReason = "Needs at least 3 words with definitions",
        plate = colors.plateSage,
        onClick = { onStartSession(StudyMode.MEANING_MATCH) },
    )
    val spelling = Activity(
        art = Illustration.ListenAndSpell,
        title = StudyMode.LISTEN_AND_SPELL.title,
        body = "Hear the word, then type it.",
        enabled = state.canSpell,
        disabledReason = "Needs a word with a definition",
        plate = colors.plateOchre,
        onClick = { onStartSession(StudyMode.LISTEN_AND_SPELL) },
    )
    // While the desk is loading, keep the usual descriptions instead of flashing "not available" reasons.
    val loading = state.loading
    BoxWithConstraints(modifier) {
        val stackPair = maxWidth < 330.dp || LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Practise")
            WideTile(flashcards, loading, artFirst = true)
            if (stackPair) {
                WideTile(meaning, loading, artFirst = false)
                WideTile(spelling, loading, artFirst = false)
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TallTile(meaning, loading, Modifier.weight(1f))
                    TallTile(spelling, loading, Modifier.weight(1f))
                }
            }
        }
    }
}

private data class Activity(
    val art: Illustration,
    val title: String,
    val body: String,
    val enabled: Boolean,
    val disabledReason: String,
    val plate: Color,
    val onClick: () -> Unit,
)

/**
 * A clickable bento tile. The label is set on the tile itself, so a screen reader still hears the
 * title and description when the tile is only partly on screen and its text is scrolled out of view.
 */
@Composable
private fun ActivityTile(activity: Activity, loading: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    val label = "${activity.title}. ${if (activity.enabled || loading) activity.body else activity.disabledReason}"
    Surface(
        onClick = activity.onClick,
        enabled = activity.enabled,
        shape = RoundedCornerShape(20.dp),
        color = activity.plate,
        border = plateBorder(),
        modifier = modifier.semantics { contentDescription = label },
        content = content,
    )
}

@Composable
private fun ActivityText(activity: Activity, loading: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(activity.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            if (activity.enabled || loading) activity.body else activity.disabledReason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WideTile(activity: Activity, loading: Boolean, artFirst: Boolean) {
    ActivityTile(activity, loading, Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = if (artFirst) 8.dp else 20.dp, end = if (artFirst) 16.dp else 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artSize = if (artFirst) 112.dp else 88.dp
            if (artFirst) BrandIllustration(activity.art, Modifier.size(artSize), enabled = activity.enabled || loading)
            ActivityText(
                activity,
                loading,
                Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
            )
            if (!artFirst) BrandIllustration(activity.art, Modifier.size(artSize), enabled = activity.enabled || loading)
            if (activity.enabled) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TallTile(activity: Activity, loading: Boolean, modifier: Modifier) {
    ActivityTile(activity, loading, modifier.fillMaxHeight()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)) {
            BrandIllustration(
                activity.art,
                Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                enabled = activity.enabled || loading,
            )
            Spacer(Modifier.height(8.dp))
            ActivityText(activity, loading)
        }
    }
}

@Composable
private fun LookUpField(onLookUp: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        label = { Text("Look up a word") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onLookUp(query.trim()) }),
    )
}

/** A short ochre rule under the hero label, like a printer's ornament. Decorative only. */
@Composable
private fun AccentRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 28.dp, height = 2.dp)
            .background(WordkeepTheme.colors.accent),
    )
}

/** Plates have no border, except in high-contrast mode where every boundary must be visible. */
@Composable
private fun plateBorder(): BorderStroke? =
    if (WordkeepTheme.accessibility.highContrast) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) else null

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

/** Font scale at which illustrations step aside so text keeps the full width. */
private const val LARGE_TEXT_SCALE = 1.6f
