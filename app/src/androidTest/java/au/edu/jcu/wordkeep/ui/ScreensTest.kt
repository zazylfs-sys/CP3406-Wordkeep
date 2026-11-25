package au.edu.jcu.wordkeep.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.stats.LearningStats
import au.edu.jcu.wordkeep.domain.stats.MasteryBreakdown
import au.edu.jcu.wordkeep.domain.stats.StatsCalculator
import au.edu.jcu.wordkeep.ui.home.HomeScreen
import au.edu.jcu.wordkeep.ui.home.HomeUiState
import au.edu.jcu.wordkeep.ui.settings.SettingsScreen
import au.edu.jcu.wordkeep.ui.stats.StatsScreen
import au.edu.jcu.wordkeep.ui.stats.StatsUiState
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 25)

    @Test
    fun home_withWorkDue_startsAFlashcardSession() {
        var started: StudyMode? = null
        compose.setContent {
            WordkeepTheme {
                HomeScreen(
                    state = HomeUiState(loading = false, today = today, dueCount = 4, newAvailable = 2, readyWords = 10),
                    wideLayout = false,
                    onStartSession = { started = it },
                    onLookUp = {},
                    onOpenLibrary = {},
                    onRetryDownload = {},
                )
            }
        }
        compose.onNodeWithText("Start today’s review").performClick()
        assertEquals(StudyMode.FLASHCARDS, started)
    }

    @Test
    fun home_disablesActivitiesWithAReasonWhenThereAreNoWords() {
        compose.setContent {
            WordkeepTheme {
                HomeScreen(HomeUiState(loading = false, today = today), false, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Add some words to begin.").assertIsDisplayed()
        compose.onNodeWithText("Needs at least 3 words with definitions").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Meaning match")).assertExists()
        compose.onAllNodesWithText("Meaning match")[0].assertExists()
    }

    @Test
    fun home_offlineModeIsAlwaysVisible() {
        compose.setContent {
            WordkeepTheme { HomeScreen(HomeUiState(loading = false, today = today, offlineMode = true), false, {}, {}, {}, {}) }
        }
        compose.onNodeWithText("Offline mode is on", substring = true).assertIsDisplayed()
    }

    @Test
    fun settings_switchRowTogglesTheSetting() {
        var settings = UserSettings()
        compose.setContent {
            WordkeepTheme {
                SettingsScreen(settings, onUpdate = { settings = it(settings) }, onOpenYourData = {})
            }
        }
        compose.onNodeWithText("Offline mode").performScrollTo().performClick()
        assertTrue(settings.offlineMode)
    }

    @Test
    fun settings_segmentsDescribeTheirGroup() {
        compose.setContent {
            WordkeepTheme { SettingsScreen(UserSettings(sessionLength = 10), {}, {}) }
        }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Session length: 10")))
            .assertExists()
    }

    @Test
    fun stats_chartsOfferATableAlternative() {
        val stats = LearningStats(
            mastery = MasteryBreakdown(new = 5, learning = 3, mastered = 2),
            lastTwoWeeks = StatsCalculator.fillDays(emptyList(), today),
            forecast = StatsCalculator.forecast(emptyList(), today),
            byMode = emptyList(),
            hardestWords = emptyList(),
            totalAnswered = 12,
            totalCorrect = 9,
            studyMinutes = 4,
            activeDaysLast30 = 3,
        )
        compose.setContent { WordkeepTheme { StatsScreen(StatsUiState.Ready(stats, today), false) {} } }

        compose.onNodeWithText("75%").assertIsDisplayed()
        compose.onAllNodesWithText("Show numbers")[0].performClick()
        compose.onNodeWithText("Answered").assertIsDisplayed()
    }
}
