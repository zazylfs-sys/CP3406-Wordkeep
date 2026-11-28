package au.edu.jcu.wordkeep.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import au.edu.jcu.wordkeep.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end journeys through the real NavHost, ViewModels, repositories and an in-memory Room
 * database. Only the network APIs are faked (see TestModules.kt).
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun finishOnboardingWithoutStarterList() {
        hilt.inject()
        compose.waitUntilAtLeastOneExists(hasText("Start learning"), 10_000)
        compose.onNodeWithText("Add the starter list").performScrollTo().performClick()
        compose.onNodeWithText("Start learning").performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasText("TODAY’S DESK"), 10_000)
    }

    private fun tab(label: String) =
        compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    @Test
    fun lookUpSaveAndStudyAWord_thenBackReturnsToToday() {
        compose.onNodeWithText("Look up a word").performScrollTo().performTextInput("hypothesis")
        compose.onNodeWithText("Look up a word").performImeAction()

        compose.waitUntilAtLeastOneExists(hasText("Save to my words"), 10_000)
        compose.onNodeWithText("Test meaning of hypothesis.").assertIsDisplayed()
        compose.onNodeWithText("Save to my words").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Saved. Open in my words"), 5_000)
        compose.onNodeWithText("Saved. Open in my words").performClick()

        // The look-up was replaced by the word page, so Back goes straight home.
        compose.waitUntilAtLeastOneExists(hasText("MEMORY HOOK"), 5_000)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Start today’s review"), 5_000)

        compose.onNodeWithText("Start today’s review").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Show meaning"), 5_000)
        compose.onNodeWithText("Show meaning").performClick()
        compose.onNode(hasText("Good") and hasClickAction()).performClick()

        // Finished sessions are removed from the back stack: the summary returns to Today.
        compose.waitUntilAtLeastOneExists(hasText("Session complete"), 5_000)
        compose.onNodeWithText("Back to Today").performClick()
        compose.waitUntilAtLeastOneExists(hasText("answered today"), 5_000)
        compose.onNodeWithText("You’re up to date.", substring = true).assertIsDisplayed()
    }

    @Test
    fun unknownWord_showsNotFoundState() {
        tab("Words").performClick()
        compose.onNodeWithText("Search or look up a word").performTextInput("qwzxv")
        compose.onNodeWithText("Look up “qwzxv”").performClick()
        compose.waitUntilAtLeastOneExists(hasText("“qwzxv” isn’t in the dictionary"), 5_000)
    }

    @Test
    fun tabsKeepTheirOwnBackStack_andBackFromATabReturnsToToday() {
        tab("Settings").performClick()
        compose.onNodeWithText("Your data").performScrollTo().performClick()
        compose.waitUntilAtLeastOneExists(hasText("Stored only on this device"), 5_000)

        tab("Progress").performClick()
        compose.waitUntilAtLeastOneExists(hasText("No progress yet"), 5_000)

        // Returning to Settings restores the screen that was open there.
        tab("Settings").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Stored only on this device"), 5_000)

        Espresso.pressBack() // Your data -> Settings
        Espresso.pressBack() // Settings -> Today
        compose.waitUntilAtLeastOneExists(hasText("TODAY’S DESK"), 5_000)
    }

    @Test
    fun todayTab_doesNotBringBackAScreenThatWasLeft() {
        compose.onNodeWithText("Look up a word").performScrollTo().performTextInput("theory")
        compose.onNodeWithText("Look up a word").performImeAction()
        compose.waitUntilAtLeastOneExists(hasText("Save to my words"), 10_000)

        tab("Today").performClick()
        compose.waitUntilAtLeastOneExists(hasText("TODAY’S DESK"), 5_000)
        tab("Progress").performClick()
        tab("Today").performClick()

        compose.waitUntilAtLeastOneExists(hasText("TODAY’S DESK"), 5_000)
        compose.onNodeWithText("Save to my words").assertDoesNotExist()
    }

    @Test
    fun emptyQuiz_explainsWhatIsNeeded() {
        compose.onNodeWithText("Meaning match").performScrollTo().performClick()
        // The row is disabled with no words, so the app stays on Today and explains why.
        compose.onNodeWithText("Needs at least 3 words with definitions").assertIsDisplayed()
    }
}
