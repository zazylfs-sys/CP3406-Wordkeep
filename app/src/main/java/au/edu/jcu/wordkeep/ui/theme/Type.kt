package au.edu.jcu.wordkeep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import au.edu.jcu.wordkeep.R

// Literata (serif) for headwords: a book face designed for long-form reading.
// IBM Plex Sans for interface text: clear I/l/1 shapes for second-language readers and
// full IPA coverage for phonetic spellings. Both are bundled variable fonts (SIL OFL 1.1).

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Literata = FontFamily(
    variable(R.font.literata, FontWeight.Normal),
    variable(R.font.literata, FontWeight.Medium),
    variable(R.font.literata, FontWeight.SemiBold),
    variable(R.font.literata, FontWeight.Bold),
)

val PlexSans = FontFamily(
    variable(R.font.ibm_plex_sans, FontWeight.Normal),
    variable(R.font.ibm_plex_sans, FontWeight.Medium),
    variable(R.font.ibm_plex_sans, FontWeight.SemiBold),
    variable(R.font.ibm_plex_sans, FontWeight.Bold),
)

private fun serif(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = Literata, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun sans(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = PlexSans,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

val WordkeepTypography = Typography(
    displayLarge = serif(52, 60),
    displayMedium = serif(42, 50),
    displaySmall = serif(34, 42),
    headlineLarge = serif(30, 38),
    headlineMedium = serif(26, 34),
    headlineSmall = serif(22, 30),
    titleLarge = serif(20, 28),
    titleMedium = sans(17, 24, FontWeight.Medium),
    titleSmall = sans(15, 22, FontWeight.Medium),
    bodyLarge = sans(16, 25),
    bodyMedium = sans(14, 21),
    bodySmall = sans(13, 19),
    labelLarge = sans(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = sans(13, 18, FontWeight.Medium, 0.2),
    labelSmall = sans(12, 16, FontWeight.Medium, 0.4),
)
