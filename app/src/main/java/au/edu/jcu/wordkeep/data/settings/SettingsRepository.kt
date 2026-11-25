package au.edu.jcu.wordkeep.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import au.edu.jcu.wordkeep.domain.model.Accent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
    suspend fun clear()
}

/**
 * Preferences DataStore suits these small key-value settings; structured learning data is in Room.
 * Unknown or corrupted enum values fall back to defaults instead of crashing.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.onboarding] = updated.onboardingComplete
            prefs[Keys.dailyNewWords] = updated.dailyNewWords
            prefs[Keys.sessionLength] = updated.sessionLength
            prefs[Keys.spellingHints] = updated.spellingHints
            prefs[Keys.soundEffects] = updated.soundEffects
            prefs[Keys.autoPlay] = updated.autoPlayPronunciation
            prefs[Keys.accent] = updated.accent.name
            prefs[Keys.haptics] = updated.haptics
            prefs[Keys.themeMode] = updated.themeMode.name
            prefs[Keys.highContrast] = updated.highContrast
            prefs[Keys.textScale] = updated.textScale.name
            prefs[Keys.reduceMotion] = updated.reduceMotion
            prefs[Keys.offlineMode] = updated.offlineMode
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            onboardingComplete = this[Keys.onboarding] ?: defaults.onboardingComplete,
            dailyNewWords = this[Keys.dailyNewWords] ?: defaults.dailyNewWords,
            sessionLength = this[Keys.sessionLength] ?: defaults.sessionLength,
            spellingHints = this[Keys.spellingHints] ?: defaults.spellingHints,
            soundEffects = this[Keys.soundEffects] ?: defaults.soundEffects,
            autoPlayPronunciation = this[Keys.autoPlay] ?: defaults.autoPlayPronunciation,
            accent = enumOrDefault(this[Keys.accent], defaults.accent),
            haptics = this[Keys.haptics] ?: defaults.haptics,
            themeMode = enumOrDefault(this[Keys.themeMode], defaults.themeMode),
            highContrast = this[Keys.highContrast] ?: defaults.highContrast,
            textScale = enumOrDefault(this[Keys.textScale], defaults.textScale),
            reduceMotion = this[Keys.reduceMotion] ?: defaults.reduceMotion,
            offlineMode = this[Keys.offlineMode] ?: defaults.offlineMode,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val dailyNewWords = intPreferencesKey("daily_new_words")
        val sessionLength = intPreferencesKey("session_length")
        val spellingHints = booleanPreferencesKey("spelling_hints")
        val soundEffects = booleanPreferencesKey("sound_effects")
        val autoPlay = booleanPreferencesKey("auto_play_pronunciation")
        val accent = stringPreferencesKey("accent")
        val haptics = booleanPreferencesKey("haptics")
        val themeMode = stringPreferencesKey("theme_mode")
        val highContrast = booleanPreferencesKey("high_contrast")
        val textScale = stringPreferencesKey("text_scale")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val offlineMode = booleanPreferencesKey("offline_mode")
    }
}
