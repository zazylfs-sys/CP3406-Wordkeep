package au.edu.jcu.wordkeep.ui.session

import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.quiz.MeaningQuestion
import au.edu.jcu.wordkeep.domain.spelling.SpellingVerdict
import java.time.LocalDate

sealed interface SessionUiState {
    val mode: StudyMode

    data class Loading(override val mode: StudyMode) : SessionUiState

    /** Nothing to study, with the reason and (for reviews) when the next one is due. */
    data class Empty(
        override val mode: StudyMode,
        val reason: EmptyReason,
        val nextReview: LocalDate? = null,
        val today: LocalDate,
    ) : SessionUiState

    data class Active(
        override val mode: StudyMode,
        /** 0-based position of the current item. */
        val position: Int,
        val total: Int,
        val item: SessionItem,
    ) : SessionUiState {
        /** Matches the "3 of 10" label: the current item counts as reached. */
        val progress: Float get() = (position + 1).toFloat() / total.coerceAtLeast(1)
    }

    data class Finished(override val mode: StudyMode, val summary: SessionSummary) : SessionUiState
}

enum class EmptyReason { NOTHING_DUE, NOT_ENOUGH_WORDS, NO_WORDS }

sealed interface SessionItem {
    val word: StudyWord

    data class Flashcard(
        override val word: StudyWord,
        val revealed: Boolean,
        /** Days until the next review for each grade, shown on the grade buttons. */
        val intervals: Map<Grade, Int>,
        /** True for a second look at a card marked "Again" earlier in the session. */
        val isRepeat: Boolean,
    ) : SessionItem

    data class Meaning(
        override val word: StudyWord,
        val question: MeaningQuestion,
        val selectedIndex: Int? = null,
    ) : SessionItem {
        val answered: Boolean get() = selectedIndex != null
        val isCorrect: Boolean get() = selectedIndex == question.answerIndex
    }

    data class Spelling(
        override val word: StudyWord,
        val attempt: String = "",
        val attemptsUsed: Int = 0,
        val verdict: SpellingVerdict? = null,
        val finished: Boolean = false,
        val hintVisible: Boolean = false,
    ) : SessionItem {
        val canRetry: Boolean get() = verdict is SpellingVerdict.NearMiss && !finished
    }
}

data class SessionSummary(
    val answered: Int,
    val correct: Int,
    val seconds: Int,
    val missedWordIds: List<Long>,
)
