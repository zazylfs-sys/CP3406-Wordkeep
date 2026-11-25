package au.edu.jcu.wordkeep.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.domain.model.MasteryLevel
import au.edu.jcu.wordkeep.domain.stats.LearningStats
import au.edu.jcu.wordkeep.domain.stats.MasteryBreakdown
import au.edu.jcu.wordkeep.ui.components.BarChart
import au.edu.jcu.wordkeep.ui.components.BarDatum
import au.edu.jcu.wordkeep.ui.components.DataTable
import au.edu.jcu.wordkeep.ui.components.EmptyState
import au.edu.jcu.wordkeep.ui.components.Hairline
import au.edu.jcu.wordkeep.ui.components.LegendItem
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.SerifHeading
import au.edu.jcu.wordkeep.ui.components.StatFigure
import au.edu.jcu.wordkeep.ui.friendly
import au.edu.jcu.wordkeep.ui.plural
import au.edu.jcu.wordkeep.ui.shortDay
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme
import java.time.LocalDate

@Composable
fun StatsRoute(
    onOpenWord: (Long) -> Unit,
    wideLayout: Boolean,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state, wideLayout, onOpenWord)
}

/** Progress at a glance. Every chart has a "Show numbers" table for screen-reader users. */
@Composable
fun StatsScreen(state: StatsUiState, wideLayout: Boolean, onOpenWord: (Long) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SerifHeading("Progress")
        when (state) {
            StatsUiState.Loading -> CircularProgressIndicator()
            is StatsUiState.Ready -> if (state.stats.isEmpty) {
                EmptyState(
                    icon = Icons.Rounded.Insights,
                    title = "No progress yet",
                    body = "Statistics appear after your first session. They are worked out on this device and never uploaded.",
                )
            } else if (wideLayout) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Overview(state.stats)
                        MasteryCard(state.stats.mastery)
                        ModeCard(state.stats)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        ActivityCard(state.stats, state.today)
                        ForecastCard(state.stats, state.today)
                        HardestCard(state.stats, onOpenWord)
                    }
                }
            } else {
                Overview(state.stats)
                MasteryCard(state.stats.mastery)
                ActivityCard(state.stats, state.today)
                ForecastCard(state.stats, state.today)
                ModeCard(state.stats)
                HardestCard(state.stats, onOpenWord)
            }
        }
    }
}

@Composable
private fun Overview(stats: LearningStats) {
    PaperCard(Modifier.fillMaxWidth()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatFigure(stats.mastery.total.toString(), "words saved")
            StatFigure(stats.overallAccuracy?.let { "$it%" } ?: "—", "answers correct")
            StatFigure(stats.activeDaysLast30.toString(), "days studied, last 30")
            StatFigure(stats.studyMinutes.toString(), "minutes in total")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Days studied counts any day you practised. There is no streak to lose.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MasteryCard(mastery: MasteryBreakdown) {
    val colors = WordkeepTheme.colors
    val segments = listOf(
        Triple(MasteryLevel.MASTERED, mastery.mastered, colors.mastered),
        Triple(MasteryLevel.LEARNING, mastery.learning, colors.learning),
        Triple(MasteryLevel.NEW, mastery.new, colors.newWord),
    )
    PaperCard(Modifier.fillMaxWidth()) {
        SectionLabel("Mastery")
        Spacer(Modifier.height(12.dp))
        val summary = segments.joinToString(", ") { (level, count, _) -> "$count ${level.label.lowercase()}" }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clearAndSetSemantics { contentDescription = "Mastery: $summary" },
        ) {
            val total = mastery.total.coerceAtLeast(1)
            var x = 0f
            val gap = 2.dp.toPx()
            segments.forEach { (_, count, color) ->
                if (count == 0) return@forEach
                val width = size.width * count / total
                drawRoundRect(color, Offset(x, 0f), Size((width - gap).coerceAtLeast(1f), size.height), CornerRadius(4.dp.toPx()))
                x += width
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            segments.forEach { (level, count, color) -> LegendItem(color, "${level.label} $count") }
        }
        Text(
            "Mastered means the next review is at least three weeks away.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ChartCard(
    title: String,
    table: @Composable () -> Unit,
    chart: @Composable () -> Unit,
) {
    var showTable by rememberSaveable(title) { mutableStateOf(false) }
    PaperCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(title, Modifier.weight(1f))
            val action = if (showTable) "Show chart" else "Show numbers"
            // Each card's button names its chart, so TalkBack users can tell them apart.
            TextButton(
                onClick = { showTable = !showTable },
                modifier = Modifier.semantics { contentDescription = "$action: $title" },
            ) { Text(action) }
        }
        Spacer(Modifier.height(8.dp))
        if (showTable) table() else chart()
    }
}

@Composable
private fun ActivityCard(stats: LearningStats, today: LocalDate) {
    val days = stats.lastTwoWeeks
    val answered = days.sumOf { it.answered }
    ChartCard(
        title = "Last 14 days",
        table = {
            DataTable(
                headers = listOf("Day", "Answered", "Correct"),
                rows = days.reversed().map { listOf(it.date.friendly(today), it.answered.toString(), it.correct.toString()) },
            )
        },
        chart = {
            BarChart(
                bars = days.map { BarDatum(it.date.shortDay(), it.answered, it.correct) },
                summary = "Last 14 days: ${plural(answered, "answer")}, ${days.sumOf { it.correct }} correct, on ${days.count { it.answered > 0 }} days.",
                barColor = WordkeepTheme.colors.correct,
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(WordkeepTheme.colors.correct, "Correct")
                LegendItem(WordkeepTheme.colors.chartTrack, "Not correct")
            }
        },
    )
}

@Composable
private fun ForecastCard(stats: LearningStats, today: LocalDate) {
    val forecast = stats.forecast
    ChartCard(
        title = "Reviews coming up",
        table = {
            DataTable(
                headers = listOf("Day", "Reviews due"),
                rows = forecast.map { listOf(it.date.friendly(today), it.count.toString()) },
            )
        },
        chart = {
            BarChart(
                bars = forecast.mapIndexed { i, f -> BarDatum(if (i == 0) "Today" else f.date.shortDay(), f.count) },
                summary = "Reviews due over the next 7 days: " + forecast.joinToString(", ") { "${it.date.friendly(today)} ${it.count}" },
                barColor = MaterialTheme.colorScheme.primary,
                backColor = MaterialTheme.colorScheme.primary,
                height = 100.dp,
            )
        },
    )
}

@Composable
private fun ModeCard(stats: LearningStats) {
    PaperCard(Modifier.fillMaxWidth()) {
        SectionLabel("Accuracy by activity")
        Spacer(Modifier.height(8.dp))
        if (stats.byMode.isEmpty()) {
            Text("No answers yet.", style = MaterialTheme.typography.bodyMedium)
        }
        stats.byMode.forEach { mode ->
            val percent = mode.accuracyPercent ?: 0
            Column(
                Modifier
                    .padding(vertical = 6.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Row {
                    Text(mode.mode.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("$percent% of ${mode.answered}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                    trackColor = WordkeepTheme.colors.chartTrack,
                )
            }
        }
    }
}

@Composable
private fun HardestCard(stats: LearningStats, onOpenWord: (Long) -> Unit) {
    PaperCard(Modifier.fillMaxWidth()) {
        SectionLabel("Words that need care")
        Spacer(Modifier.height(4.dp))
        if (stats.hardestWords.isEmpty()) {
            Text(
                "Words you miss more than once will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        stats.hardestWords.forEachIndexed { index, word ->
            if (index > 0) Hairline()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(onClickLabel = "Open ${word.text}") { onOpenWord(word.wordId) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(word.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "${word.accuracyPercent}% of ${word.attempts}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
