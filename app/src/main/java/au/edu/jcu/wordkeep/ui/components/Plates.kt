package au.edu.jcu.wordkeep.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme

/** Font scale at which illustrations step aside or shrink, so text keeps the full width. */
const val LARGE_TEXT_SCALE = 1.6f

@Composable
fun isLargeText(): Boolean = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE

/** Plates have no border, except in high-contrast mode where every boundary must be visible. */
@Composable
fun plateBorder(): BorderStroke? =
    if (WordkeepTheme.accessibility.highContrast) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) else null

/** A short ochre rule under a label, like a printer's ornament. Decorative only. */
@Composable
fun AccentRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 28.dp, height = 2.dp)
            .background(WordkeepTheme.colors.accent),
    )
}

/**
 * The illustrated "plate" used at the top of key screens: a tinted card with a label, an ochre
 * rule and a headline on the left, the illustration at the top end, and any further [content]
 * across the full width below. The illustration sits beside the text, never behind it, and is left
 * out with very large text.
 */
@Composable
fun IllustratedPlate(
    art: Illustration,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = WordkeepTheme.colors.plateHero,
    artSize: Dp = 148.dp,
    headline: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(24.dp),
        border = plateBorder(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    SectionLabel(label)
                    AccentRule(Modifier.padding(top = 8.dp, bottom = 12.dp))
                    headline()
                }
                if (!isLargeText()) {
                    BrandIllustration(
                        art,
                        Modifier
                            .padding(start = 8.dp)
                            .size(artSize)
                            .offset(x = 12.dp, y = (-8).dp),
                    )
                }
            }
            content()
        }
    }
}

/** Each activity keeps one illustration and one plate colour everywhere it appears. */
val StudyMode.illustration: Illustration
    get() = when (this) {
        StudyMode.FLASHCARDS -> Illustration.Flashcards
        StudyMode.MEANING_MATCH -> Illustration.MeaningMatch
        StudyMode.LISTEN_AND_SPELL -> Illustration.ListenAndSpell
    }

@Composable
fun StudyMode.plateColor(): Color = when (this) {
    StudyMode.FLASHCARDS -> WordkeepTheme.colors.plateCream
    StudyMode.MEANING_MATCH -> WordkeepTheme.colors.plateSage
    StudyMode.LISTEN_AND_SPELL -> WordkeepTheme.colors.plateOchre
}
