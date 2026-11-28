package au.edu.jcu.wordkeep.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// "Print lexicon" palette: forest bookcloth, walnut sepia, claret, archival paper.
// Every text/background pair used in the app is at least 4.5:1 (WCAG 2.2 AA); most exceed 7:1.

val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF204E2E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EAD9),
    onPrimaryContainer = Color(0xFF12311C),
    secondary = Color(0xFF544B42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7DFD3),
    onSecondaryContainer = Color(0xFF241E19),
    tertiary = Color(0xFF663354),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0DCE8),
    onTertiaryContainer = Color(0xFF331328),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1A1917),
    surface = Color(0xFFFAF7F2),
    onSurface = Color(0xFF1A1917),
    surfaceVariant = Color(0xFFEAE3D7),
    onSurfaceVariant = Color(0xFF4F4B45),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3EFE7),
    surfaceContainer = Color(0xFFEFEAE1),
    surfaceContainerHigh = Color(0xFFEAE3D7),
    surfaceContainerHighest = Color(0xFFE3DCCF),
    outline = Color(0xFF756F67),
    outlineVariant = Color(0xFFCFC7BA),
    error = Color(0xFFA32222),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDA),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2F2D2A),
    inverseOnSurface = Color(0xFFF4EFE6),
    inversePrimary = Color(0xFF8ED19E),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF8ED19E),
    onPrimary = Color(0xFF0A2913),
    primaryContainer = Color(0xFF1B3D24),
    onPrimaryContainer = Color(0xFFD6EAD9),
    secondary = Color(0xFFCFC5B7),
    onSecondary = Color(0xFF221D17),
    secondaryContainer = Color(0xFF3B352E),
    onSecondaryContainer = Color(0xFFECE4D9),
    tertiary = Color(0xFFE2A8CD),
    onTertiary = Color(0xFF36112A),
    tertiaryContainer = Color(0xFF4C223D),
    onTertiaryContainer = Color(0xFFF5E2EE),
    background = Color(0xFF141513),
    onBackground = Color(0xFFEDE8DF),
    surface = Color(0xFF141513),
    onSurface = Color(0xFFEDE8DF),
    surfaceVariant = Color(0xFF272A26),
    onSurfaceVariant = Color(0xFFBDB6AA),
    surfaceContainerLowest = Color(0xFF0F100E),
    surfaceContainerLow = Color(0xFF1B1D1A),
    surfaceContainer = Color(0xFF20221F),
    surfaceContainerHigh = Color(0xFF272A26),
    surfaceContainerHighest = Color(0xFF313430),
    outline = Color(0xFF8E887E),
    outlineVariant = Color(0xFF3E413B),
    error = Color(0xFFF29B9B),
    onError = Color(0xFF420808),
    errorContainer = Color(0xFF5C1414),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFEDE8DF),
    inverseOnSurface = Color(0xFF2F2D2A),
    inversePrimary = Color(0xFF204E2E),
)

/** High-contrast variants: pure paper/ink surfaces, darker accents and stronger outlines. */
val LightHighContrastColors: ColorScheme = LightColors.copy(
    primary = Color(0xFF0F381B),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    onBackground = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF1F1F1F),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerHighest = Color(0xFFDDDDDD),
    outline = Color(0xFF2B2B2B),
    outlineVariant = Color(0xFF595959),
    error = Color(0xFF8C0F0F),
)

val DarkHighContrastColors: ColorScheme = DarkColors.copy(
    primary = Color(0xFFA8F0B8),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFE5E5E5),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
    outline = Color(0xFFD4D4D4),
    outlineVariant = Color(0xFFA3A3A3),
    error = Color(0xFFFFB4B4),
)

/** Colours outside the Material roles. Correct/incorrect are always paired with an icon and text. */
data class WordkeepColors(
    val correct: Color,
    val onCorrect: Color,
    val correctContainer: Color,
    val incorrect: Color,
    val incorrectContainer: Color,
    val learning: Color,
    val mastered: Color,
    val newWord: Color,
    val chartTrack: Color,
    /** Tinted "plates" that carry brand illustrations: sage, cream and ochre washes of the paper. */
    val plateHero: Color,
    val plateCream: Color,
    val plateSage: Color,
    val plateOchre: Color,
    /** Ochre accent for small decorative rules only; never used for text. */
    val accent: Color,
)

val LightExtraColors = WordkeepColors(
    correct = Color(0xFF1B5E2E),
    onCorrect = Color(0xFFFFFFFF),
    correctContainer = Color(0xFFDCEFDF),
    incorrect = Color(0xFFA32222),
    incorrectContainer = Color(0xFFF9DEDA),
    learning = Color(0xFF544B42),
    mastered = Color(0xFF204E2E),
    newWord = Color(0xFF663354),
    chartTrack = Color(0xFFE3DCCF),
    plateHero = Color(0xFFE4EADD),
    plateCream = Color(0xFFF4EDDC),
    plateSage = Color(0xFFDCE5D5),
    plateOchre = Color(0xFFF2E6C8),
    accent = Color(0xFFB8862E),
)

val DarkExtraColors = WordkeepColors(
    correct = Color(0xFF85D998),
    onCorrect = Color(0xFF082911),
    correctContainer = Color(0xFF173A21),
    incorrect = Color(0xFFF29B9B),
    incorrectContainer = Color(0xFF4A1616),
    learning = Color(0xFFCFC5B7),
    mastered = Color(0xFF8ED19E),
    newWord = Color(0xFFE2A8CD),
    chartTrack = Color(0xFF313430),
    plateHero = Color(0xFF1B2921),
    plateCream = Color(0xFF25241E),
    plateSage = Color(0xFF1E2A23),
    plateOchre = Color(0xFF2C271B),
    accent = Color(0xFFD9AE58),
)

val LightHighContrastExtraColors = LightExtraColors.copy(
    correct = Color(0xFF0F461E),
    incorrect = Color(0xFF8C0F0F),
    mastered = Color(0xFF0F381B),
    learning = Color(0xFF2B2B2B),
    newWord = Color(0xFF4A1A3A),
    chartTrack = Color(0xFFCCCCCC),
    plateHero = Color(0xFFF2F2F2),
    plateCream = Color(0xFFF5F5F5),
    plateSage = Color(0xFFF5F5F5),
    plateOchre = Color(0xFFF5F5F5),
)

val DarkHighContrastExtraColors = DarkExtraColors.copy(
    correct = Color(0xFF9DF2B0),
    incorrect = Color(0xFFFFB4B4),
    mastered = Color(0xFFA8F0B8),
    learning = Color(0xFFE5E5E5),
    newWord = Color(0xFFF5C6E4),
    chartTrack = Color(0xFF3A3A3A),
    plateHero = Color(0xFF111111),
    plateCream = Color(0xFF0D0D0D),
    plateSage = Color(0xFF0D0D0D),
    plateOchre = Color(0xFF0D0D0D),
)
