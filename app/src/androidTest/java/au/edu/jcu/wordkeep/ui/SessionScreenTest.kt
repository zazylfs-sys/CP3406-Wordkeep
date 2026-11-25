package au.edu.jcu.wordkeep.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.quiz.MeaningQuestion
import au.edu.jcu.wordkeep.domain.spelling.LetterMark
import au.edu.jcu.wordkeep.domain.spelling.SpellingVerdict
import au.edu.jcu.wordkeep.ui.session.SessionActions
import au.edu.jcu.wordkeep.ui.session.SessionItem
import au.edu.jcu.wordkeep.ui.session.SessionScreen
import au.edu.jcu.wordkeep.ui.session.SessionUiState
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** GUI tests for the activity screen, including the accessibility details it promises. */
class SessionScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val word = studyWord(1, "hypothesis")
    private val intervals = mapOf(Grade.AGAIN to 1, Grade.HARD to 1, Grade.GOOD to 1, Grade.EASY to 1)

    private fun show(state: SessionUiState, actions: SessionActions = SessionActions()) {
        compose.setContent {
            WordkeepTheme(reduceMotion = true) {
                SessionScreen(state, PlaybackState.IDLE, UserSettings(), actions, onExit = {}, onOpenLibrary = {})
            }
        }
    }

    @Test
    fun flashcardFront_showsWordAndRevealButton_butNotGrades() {
        show(SessionUiState.Active(StudyMode.FLASHCARDS, 0, 5, SessionItem.Flashcard(word, false, intervals, false)))

        compose.onNodeWithText("hypothesis").assertIsDisplayed()
        compose.onNodeWithText("1 of 5").assertIsDisplayed()
        compose.onNodeWithText("Show meaning").assertIsDisplayed()
        compose.onNodeWithText("Good").assertDoesNotExist()
        compose.onNodeWithContentDescription("Play pronunciation of hypothesis").assertIsDisplayed()
    }

    @Test
    fun revealButton_callsReveal() {
        var revealed = false
        show(
            SessionUiState.Active(StudyMode.FLASHCARDS, 0, 5, SessionItem.Flashcard(word, false, intervals, false)),
            SessionActions(onReveal = { revealed = true }),
        )
        compose.onNodeWithText("Show meaning").performClick()
        assertEquals(true, revealed)
    }

    @Test
    fun flashcardBack_gradeButtonsAreLabelledAndLargeEnough() {
        var graded: Grade? = null
        show(
            SessionUiState.Active(StudyMode.FLASHCARDS, 0, 5, SessionItem.Flashcard(word, true, intervals, false)),
            SessionActions(onGrade = { graded = it }),
        )
        compose.onNodeWithText("A test definition of hypothesis.").assertIsDisplayed()
        val good = compose.onNodeWithContentDescription("Good. Next review in 1 day")
        good.assertHeightIsAtLeast(48.dp)
        good.performClick()
        assertEquals(Grade.GOOD, graded)
    }

    @Test
    fun meaningMatch_wrongAnswerIsMarkedWithTextNotJustColour() {
        val question = MeaningQuestion(word.word, word.word.senses.first(), "A ____ test", listOf("thesis", "hypothesis", "theory"), 1)
        show(SessionUiState.Active(StudyMode.MEANING_MATCH, 2, 5, SessionItem.Meaning(word, question, selectedIndex = 0)))

        compose.onNodeWithText("Your answer").assertIsDisplayed()
        compose.onNodeWithText("Correct answer").assertIsDisplayed()
        compose.onNodeWithText("Not quite. The answer is “hypothesis”.").assertIsDisplayed()
        compose.onNodeWithText("thesis").assertExists()
        compose.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun meaningMatch_choosingAnOptionReportsItsIndex() {
        var chosen = -1
        val question = MeaningQuestion(word.word, word.word.senses.first(), "A ____ test", listOf("thesis", "hypothesis", "theory"), 1)
        show(
            SessionUiState.Active(StudyMode.MEANING_MATCH, 0, 5, SessionItem.Meaning(word, question)),
            SessionActions(onChoose = { chosen = it }),
        )
        compose.onNodeWithText("theory").performClick()
        assertEquals(2, chosen)
    }

    @Test
    fun spelling_typingAndCheckingCallsTheViewModel() {
        var typed = ""
        var checked = false
        show(
            SessionUiState.Active(StudyMode.LISTEN_AND_SPELL, 0, 5, SessionItem.Spelling(word, attempt = "hypo")),
            SessionActions(onAttemptChange = { typed = it }, onSubmitSpelling = { checked = true }),
        )
        compose.onNodeWithText("Your spelling").performTextInput("t")
        compose.onNodeWithText("Check").performClick()
        assertEquals(true, typed.isNotEmpty())
        assertEquals(true, checked)
    }

    @Test
    fun spelling_fieldIsNotAPasswordForScreenReaders() {
        show(SessionUiState.Active(StudyMode.LISTEN_AND_SPELL, 0, 5, SessionItem.Spelling(word, attempt = "hypo")))
        // The keyboard is password-type (no suggestions), but TalkBack must still read the letters.
        compose.onNodeWithText("Your spelling")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
        compose.onNodeWithText("hypo").assertIsDisplayed()
    }

    @Test
    fun spelling_nearMissFeedbackIsAnnouncedPolitely() {
        val nearMiss = SpellingVerdict.NearMiss(1, listOf(LetterMark('a', LetterMark.Status.MATCHED)))
        show(
            SessionUiState.Active(
                StudyMode.LISTEN_AND_SPELL, 0, 5,
                SessionItem.Spelling(word, attempt = "hypothesys", attemptsUsed = 1, verdict = nearMiss),
            ),
        )
        compose.onNodeWithText("Very close", substring = true)
            .assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertExists()
        compose.onNodeWithText("Check again").assertIsDisplayed()
    }

    @Test
    fun progressBar_describesPositionForScreenReaders() {
        show(SessionUiState.Active(StudyMode.FLASHCARDS, 3, 8, SessionItem.Flashcard(word, false, intervals, false)))
        compose.onNodeWithContentDescription("Question 4 of 8").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
    }
}
