package au.edu.jcu.wordkeep.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import au.edu.jcu.wordkeep.data.settings.TextScale
import au.edu.jcu.wordkeep.data.settings.ThemeMode

/** App-wide accessibility preferences that individual components need to respect. */
data class AccessibilityPrefs(
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
)

val LocalWordkeepColors = staticCompositionLocalOf { LightExtraColors }
val LocalAccessibilityPrefs = staticCompositionLocalOf { AccessibilityPrefs() }

/** Small corner radii: index cards and dictionary pages rather than bubbly "app" shapes. */
private val WordkeepShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/**
 * Applies the Wordkeep theme.
 *
 * Dynamic colour is not used, because correct/incorrect and mastery colours must keep their
 * tested contrast. [textScale] multiplies the system font scale rather than replacing it, so a
 * learner who already uses large system text still gets it.
 */
@Composable
fun WordkeepTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    highContrast: Boolean = false,
    textScale: TextScale = TextScale.DEFAULT,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dark && highContrast -> DarkHighContrastColors
        dark -> DarkColors
        highContrast -> LightHighContrastColors
        else -> LightColors
    }
    val extraColors = when {
        dark && highContrast -> DarkHighContrastExtraColors
        dark -> DarkExtraColors
        highContrast -> LightHighContrastExtraColors
        else -> LightExtraColors
    }
    val systemDensity = LocalDensity.current
    val density = remember(systemDensity, textScale) {
        Density(systemDensity.density, systemDensity.fontScale * textScale.factor)
    }
    val prefs = AccessibilityPrefs(
        reduceMotion = reduceMotion || systemAnimationsOff(),
        highContrast = highContrast,
    )

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalWordkeepColors provides extraColors,
        LocalAccessibilityPrefs provides prefs,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WordkeepTypography,
            shapes = WordkeepShapes,
            content = content,
        )
    }
}

/** True when the system "Remove animations" setting is on (animator duration scale 0). */
@Composable
private fun systemAnimationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

object WordkeepTheme {
    val colors: WordkeepColors
        @Composable @ReadOnlyComposable get() = LocalWordkeepColors.current

    val accessibility: AccessibilityPrefs
        @Composable @ReadOnlyComposable get() = LocalAccessibilityPrefs.current
}
