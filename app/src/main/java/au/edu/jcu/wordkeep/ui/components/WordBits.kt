package au.edu.jcu.wordkeep.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.domain.model.MasteryLevel
import au.edu.jcu.wordkeep.domain.model.Sense
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme

/**
 * Plays a word. The label names the word ("Play pronunciation of analyse") and the state is
 * announced, so TalkBack users know whether audio is loading or playing.
 */
@Composable
fun PronounceButton(
    word: String,
    state: PlaybackState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = WordkeepTheme.accessibility.reduceMotion
    val pulse = if (state == PlaybackState.PLAYING && !reduceMotion) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
            label = "pulseAlpha",
        ).value
    } else {
        1f
    }
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier
            .size(52.dp)
            .semantics {
                contentDescription = "Play pronunciation of $word"
                stateDescription = when (state) {
                    PlaybackState.IDLE -> "Ready"
                    PlaybackState.LOADING -> "Loading audio"
                    PlaybackState.PLAYING -> "Playing"
                }
            },
    ) {
        if (state == PlaybackState.LOADING) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.alpha(pulse))
        }
    }
}

/** Mastery as icon + word + colour, so it never depends on colour alone. */
@Composable
fun MasteryTag(level: MasteryLevel, modifier: Modifier = Modifier) {
    val colors = WordkeepTheme.colors
    val (icon, tint) = when (level) {
        MasteryLevel.NEW -> Icons.Rounded.Bolt to colors.newWord
        MasteryLevel.LEARNING -> Icons.Rounded.HourglassTop to colors.learning
        MasteryLevel.MASTERED -> Icons.Rounded.Spa to colors.mastered
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(level.label, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

/** Part of speech in italics, as a printed dictionary shows it. */
@Composable
fun PartOfSpeech(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier,
    )
}

/**
 * The IPA string, labelled so screen-reader users know what the symbols are. The pronunciation
 * button next to it is the easier alternative for hearing the word.
 */
@Composable
fun PhoneticText(phonetic: String?, modifier: Modifier = Modifier) {
    if (phonetic.isNullOrBlank()) return
    Text(
        text = phonetic,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { contentDescription = "Phonetic spelling: $phonetic" },
    )
}

/** Numbered senses with hanging numbers and indented examples, like a printed dictionary entry. */
@Composable
fun SenseList(senses: List<Sense>, modifier: Modifier = Modifier, showSynonyms: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        senses.forEachIndexed { index, sense ->
            Row(Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(min = 28.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PartOfSpeech(sense.partOfSpeech)
                    Text(sense.definition, style = MaterialTheme.typography.bodyLarge)
                    sense.example?.let {
                        Text(
                            "“$it”",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showSynonyms && sense.synonyms.isNotEmpty()) {
                        Text(
                            "Similar: " + sense.synonyms.take(4).joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
