package au.edu.jcu.wordkeep.domain.model

import java.time.LocalDate

/**
 * SM-2 scheduling state for one word.
 *
 * [dueDate] is `null` until the word is first studied, which is how "new" words are told apart
 * from words in review.
 */
data class ReviewState(
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = INITIAL_EASE,
    val dueDate: LocalDate? = null,
    val lapses: Int = 0,
    val introducedOn: LocalDate? = null,
) {
    val isNew: Boolean get() = dueDate == null

    fun isDue(today: LocalDate): Boolean = dueDate != null && !dueDate.isAfter(today)

    val mastery: MasteryLevel
        get() = when {
            isNew -> MasteryLevel.NEW
            intervalDays >= MASTERED_INTERVAL_DAYS -> MasteryLevel.MASTERED
            else -> MasteryLevel.LEARNING
        }

    companion object {
        const val INITIAL_EASE = 2.5

        /** A card is "mastered" once it is scheduled three weeks or more ahead. */
        const val MASTERED_INTERVAL_DAYS = 21
    }
}

enum class MasteryLevel(val label: String) { NEW("New"), LEARNING("Learning"), MASTERED("Mastered") }

/**
 * How well the learner recalled a word. [quality] is the SM-2 response quality (0-5);
 * anything below 3 counts as a failed recall.
 */
enum class Grade(val label: String, val quality: Int) {
    AGAIN("Again", 2),
    HARD("Hard", 3),
    GOOD("Good", 4),
    EASY("Easy", 5);

    val isPass: Boolean get() = quality >= 3
}

/** The three learning activities. Stored by name in the review log. */
enum class StudyMode(val title: String, val shortTitle: String) {
    FLASHCARDS("Flashcard review", "Flashcards"),
    MEANING_MATCH("Meaning match", "Meaning"),
    LISTEN_AND_SPELL("Listen & spell", "Spelling"),
}

/** A saved word together with its learning progress. */
data class StudyWord(
    val word: Word,
    val review: ReviewState,
)
