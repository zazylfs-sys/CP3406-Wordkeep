package au.edu.jcu.wordkeep.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import au.edu.jcu.wordkeep.R

/**
 * The Wordkeep illustration set: engraved still-lifes in ink green, sage, cream and ochre.
 * Masters, prompts and the export script live in `assets/illustrations/` at the repository root;
 * the app ships the exported transparent WebPs from `res/drawable-nodpi`, so nothing is fetched.
 */
enum class Illustration(@DrawableRes val res: Int) {
    TodayDesk(R.drawable.illus_today_hero),
    Flashcards(R.drawable.illus_flashcards),
    MeaningMatch(R.drawable.illus_meaning),
    ListenAndSpell(R.drawable.illus_spelling),
    DayComplete(R.drawable.illus_done),
    Offline(R.drawable.illus_offline),
    EmptyLibrary(R.drawable.illus_empty_library),
    Growth(R.drawable.illus_stats),
    Welcome(R.drawable.illus_onboarding),
}

/**
 * Draws a brand illustration. Illustrations are decorative, so they are hidden from screen readers.
 * On dark surfaces the cream fills are dimmed slightly so they do not glare; a disabled tile's
 * illustration is shown in greyscale and faded.
 */
@Composable
fun BrandIllustration(
    art: Illustration,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val filter = when {
        !enabled -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        dark -> ColorFilter.colorMatrix(DIM_FOR_DARK)
        else -> null
    }
    Image(
        painter = painterResource(art.res),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = filter,
        alpha = if (enabled) 1f else 0.45f,
    )
}

private val DIM_FOR_DARK = ColorMatrix(
    floatArrayOf(
        0.86f, 0f, 0f, 0f, 0f,
        0f, 0.86f, 0f, 0f, 0f,
        0f, 0f, 0.86f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ),
)
