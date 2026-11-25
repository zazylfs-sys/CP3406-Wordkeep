package au.edu.jcu.wordkeep.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.activity.ComponentActivity
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.quiz.MeaningQuestion
import au.edu.jcu.wordkeep.domain.stats.LearningStats
import au.edu.jcu.wordkeep.domain.stats.MasteryBreakdown
import au.edu.jcu.wordkeep.domain.stats.StatsCalculator
import au.edu.jcu.wordkeep.ui.home.HomeScreen
import au.edu.jcu.wordkeep.ui.home.HomeUiState
import au.edu.jcu.wordkeep.ui.onboarding.OnboardingScreen
import au.edu.jcu.wordkeep.ui.session.SessionActions
import au.edu.jcu.wordkeep.ui.session.SessionItem
import au.edu.jcu.wordkeep.ui.session.SessionScreen
import au.edu.jcu.wordkeep.ui.session.SessionUiState
import au.edu.jcu.wordkeep.ui.settings.SettingsScreen
import au.edu.jcu.wordkeep.ui.stats.StatsScreen
import au.edu.jcu.wordkeep.ui.stats.StatsUiState
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Runs Google's Accessibility Test Framework over each main screen, in light and dark themes.
 * ATF fails the test on missing labels, touch targets under 48dp and low text contrast.
 */
@OptIn(ExperimentalTestApi::class)
class AccessibilityChecksTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 11, 25)
    private val word = studyWord(1, "hypothesis")

    @Before
    fun enableChecks() = compose.enableAccessibilityChecks()

    private fun check(dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            WordkeepTheme(
                themeMode = if (dark) au.edu.jcu.wordkeep.data.settings.ThemeMode.DARK
                else au.edu.jcu.wordkeep.data.settings.ThemeMode.LIGHT,
            ) { content() }
        }
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun onboarding() = check { OnboardingScreen { _, _ -> } }

    @Test
    fun home_light() = check {
        HomeScreen(HomeUiState(loading = false, today = today, dueCount = 3, newAvailable = 2, readyWords = 9), false, {}, {}, {}, {})
    }

    @Test
    fun home_dark() = check(dark = true) {
        HomeScreen(HomeUiState(loading = false, today = today, dueCount = 3, newAvailable = 2, readyWords = 9), false, {}, {}, {}, {})
    }

    @Test
    fun flashcard_back() = check {
        val intervals = mapOf(Grade.AGAIN to 1, Grade.HARD to 1, Grade.GOOD to 1, Grade.EASY to 4)
        SessionScreen(
            SessionUiState.Active(StudyMode.FLASHCARDS, 0, 5, SessionItem.Flashcard(word, true, intervals, false)),
            PlaybackState.IDLE, UserSettings(), SessionActions(), {}, {},
        )
    }

    @Test
    fun meaningMatch_answered_dark() = check(dark = true) {
        val question = MeaningQuestion(word.word, word.word.senses.first(), "A ____ test", listOf("thesis", "hypothesis", "theory"), 1)
        SessionScreen(
            SessionUiState.Active(StudyMode.MEANING_MATCH, 0, 5, SessionItem.Meaning(word, question, selectedIndex = 0)),
            PlaybackState.IDLE, UserSettings(), SessionActions(), {}, {},
        )
    }

    @Test
    fun spelling() = check {
        SessionScreen(
            SessionUiState.Active(StudyMode.LISTEN_AND_SPELL, 0, 5, SessionItem.Spelling(word, hintVisible = true)),
            PlaybackState.IDLE, UserSettings(), SessionActions(), {}, {},
        )
    }

    @Test
    fun settings() = check { SettingsScreen(UserSettings(), {}, {}) }

    @Test
    fun stats() = check {
        val stats = LearningStats(
            mastery = MasteryBreakdown(5, 3, 2),
            lastTwoWeeks = StatsCalculator.fillDays(emptyList(), today),
            forecast = StatsCalculator.forecast(emptyList(), today),
            byMode = emptyList(), hardestWords = emptyList(),
            totalAnswered = 12, totalCorrect = 9, studyMinutes = 4, activeDaysLast30 = 3,
        )
        StatsScreen(StatsUiState.Ready(stats, today), false) {}
    }
}
