package au.edu.jcu.wordkeep.data.settings

import au.edu.jcu.wordkeep.domain.model.Accent

/** Learner preferences, stored with Preferences DataStore. */
data class UserSettings(
    val onboardingComplete: Boolean = false,
    // Study
    val dailyNewWords: Int = 10,
    /** Questions or cards per session. Sessions always end; there is no endless mode. */
    val sessionLength: Int = 10,
    val spellingHints: Boolean = true,
    // Sound
    val soundEffects: Boolean = true,
    val autoPlayPronunciation: Boolean = false,
    val accent: Accent = Accent.UK,
    val haptics: Boolean = true,
    // Display and accessibility
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val highContrast: Boolean = false,
    val textScale: TextScale = TextScale.DEFAULT,
    val reduceMotion: Boolean = false,
    // Privacy
    val offlineMode: Boolean = false,
) {
    companion object {
        val DAILY_NEW_WORD_CHOICES = listOf(5, 10, 15, 20)
        val SESSION_LENGTH_CHOICES = listOf(5, 10, 15)
    }
}

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Extra text scaling on top of the system font size, for learners who want larger definitions. */
enum class TextScale(val label: String, val factor: Float) {
    DEFAULT("Default", 1f),
    LARGE("Large", 1.15f),
    LARGEST("Largest", 1.3f),
}
