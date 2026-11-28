package au.edu.jcu.wordkeep.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.stats.StatsCalculator
import au.edu.jcu.wordkeep.ui.components.Hairline
import au.edu.jcu.wordkeep.ui.components.IllustratedPlate
import au.edu.jcu.wordkeep.ui.components.Illustration
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.StatFigure
import au.edu.jcu.wordkeep.ui.navigation.SummaryRoute
import au.edu.jcu.wordkeep.ui.plural
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class SummaryUiState(
    val mode: StudyMode,
    val answered: Int,
    val correct: Int,
    val seconds: Int,
    val missedWords: List<Word> = emptyList(),
    val dueTomorrow: Int = 0,
) {
    val accuracy: Int? get() = StatsCalculator.percent(correct, answered)
}

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    wordRepository: WordRepository,
    clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SummaryRoute>()
    private val tomorrow = LocalDate.now(clock).plusDays(1)
    private val initial = SummaryUiState(route.mode, route.answered, route.correct, route.seconds)

    val uiState: StateFlow<SummaryUiState> = wordRepository.observeLibrary().map { words ->
        val missed = route.missedWordIds.toSet()
        initial.copy(
            missedWords = words.map { it.word }.filter { it.id in missed },
            dueTomorrow = words.count { it.word.isReady && it.review.dueDate?.let { d -> !d.isAfter(tomorrow) } == true },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}

@Composable
fun SummaryRoute(
    onDone: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenWord: (Long) -> Unit,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SummaryScreen(state, onDone, onOpenProgress, onOpenWord)
}

/**
 * The end of a session. It closes the loop clearly ("that's it for now") instead of offering
 * another round, so the app does not keep the learner tapping past the point of useful practice.
 */
@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onDone: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenWord: (Long) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        IllustratedPlate(
            art = Illustration.DayComplete,
            label = state.mode.title,
            headline = {
                Text(
                    "Session complete",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "That’s all for now. Spacing practice out is what moves words into long-term memory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        ) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // FlowRow: with very large text the figures wrap as whole units instead of squeezing.
                FlowRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatFigure(state.answered.toString(), "answered")
                    StatFigure(state.accuracy?.let { "$it%" } ?: "—", "correct first time")
                    StatFigure(formatDuration(state.seconds), "time")
                }
            }
        }
        if (state.missedWords.isNotEmpty()) {
            Column {
                SectionLabel("Worth another look")
                state.missedWords.forEach { word ->
                    ListItem(
                        headlineContent = { Text(word.text, style = MaterialTheme.typography.titleLarge) },
                        supportingContent = word.primarySense?.let { { Text(it.definition, maxLines = 2) } },
                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(onClickLabel = "Open word") { onOpenWord(word.id) },
                    )
                    Hairline()
                }
            }
        }
        Text(
            if (state.dueTomorrow > 0) "Next: ${plural(state.dueTomorrow, "review")} due by tomorrow."
            else "Nothing is due tomorrow.",
            style = MaterialTheme.typography.titleMedium,
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Back to Today") }
        OutlinedButton(onClick = onOpenProgress, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("See my progress")
        }
    }
}

private fun formatDuration(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
