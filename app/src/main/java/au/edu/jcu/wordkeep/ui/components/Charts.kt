package au.edu.jcu.wordkeep.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme

/** One bar: [value] is the full height, [highlight] the filled part (e.g. answered vs correct). */
data class BarDatum(val label: String, val value: Int, val highlight: Int = value)

/**
 * A simple bar chart drawn on a Canvas. The drawing is hidden from TalkBack and replaced by
 * [summary]; screens also offer a "view as table" alternative with the exact numbers.
 */
@Composable
fun BarChart(
    bars: List<BarDatum>,
    summary: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    backColor: Color = WordkeepTheme.colors.chartTrack,
    labelEvery: Int = 1,
) {
    val progress = remember { Animatable(0f) }
    val reduceMotion = WordkeepTheme.accessibility.reduceMotion
    LaunchedEffect(bars, reduceMotion) {
        if (reduceMotion) progress.snapTo(1f) else progress.animateTo(1f, tween(450))
    }
    val maxValue = (bars.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)

    Column(modifier.clearAndSetSemantics { contentDescription = summary }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            if (bars.isEmpty()) return@Canvas
            val slot = size.width / bars.size
            val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            val radius = CornerRadius(3.dp.toPx())
            bars.forEachIndexed { index, bar ->
                val left = index * slot + (slot - barWidth) / 2
                val fullHeight = size.height * bar.value / maxValue * progress.value
                val litHeight = size.height * bar.highlight / maxValue * progress.value
                // Empty days still get a short stub so the time axis reads as continuous.
                val stub = 2.dp.toPx()
                drawRoundRect(
                    color = backColor,
                    topLeft = Offset(left, size.height - fullHeight.coerceAtLeast(stub)),
                    size = Size(barWidth, fullHeight.coerceAtLeast(stub)),
                    cornerRadius = radius,
                )
                if (litHeight > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - litHeight),
                        size = Size(barWidth, litHeight),
                        cornerRadius = radius,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            bars.forEachIndexed { index, bar ->
                Text(
                    text = if (index % labelEvery == 0 || index == bars.lastIndex) bar.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Chart key entry: a colour swatch plus text. */
@Composable
fun LegendItem(color: Color, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = RoundedCornerShape(2.dp), modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A plain text table: the accessible alternative to a chart. */
@Composable
fun DataTable(headers: List<String>, rows: List<List<String>>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.padding(vertical = 4.dp)) {
            headers.forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            }
        }
        Hairline()
        rows.forEach { row ->
            Row(Modifier.padding(vertical = 4.dp)) {
                row.forEach { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)) }
            }
        }
    }
}
