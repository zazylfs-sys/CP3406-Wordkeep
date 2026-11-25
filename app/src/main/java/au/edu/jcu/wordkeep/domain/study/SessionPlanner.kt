package au.edu.jcu.wordkeep.domain.study

import au.edu.jcu.wordkeep.domain.model.StudyWord
import java.time.LocalDate

/**
 * Chooses which words go into a study session.
 *
 * Sessions are deliberately finite: due reviews first (most overdue, then hardest), then a
 * capped number of new words. When nothing is due the plan is empty and the app says so,
 * rather than inventing more work.
 */
object SessionPlanner {

    fun plan(
        words: List<StudyWord>,
        today: LocalDate,
        newWordsPerDay: Int,
        maxSize: Int,
    ): List<StudyWord> {
        val ready = words.filter { it.word.isReady }

        val due = ready
            .filter { it.review.isDue(today) }
            .sortedWith(compareBy<StudyWord> { it.review.dueDate }.thenBy { it.review.easeFactor })

        val introducedToday = ready.count { it.review.introducedOn == today }
        val newAllowance = (newWordsPerDay - introducedToday).coerceAtLeast(0)
        val fresh = ready
            .filter { it.review.isNew }
            .sortedBy { it.word.addedAt }
            .take(newAllowance)

        return (due + fresh).take(maxSize)
    }

    /** Words worth quizzing on: anything already introduced, weakest first. Never empty if any word is ready. */
    fun quizPool(words: List<StudyWord>, today: LocalDate, size: Int): List<StudyWord> {
        val ready = words.filter { it.word.isReady }
        val (studied, fresh) = ready.partition { !it.review.isNew }
        val ordered = studied.sortedWith(
            compareByDescending<StudyWord> { it.review.isDue(today) }
                .thenBy { it.review.intervalDays }
                .thenBy { it.review.easeFactor }
        ) + fresh.sortedBy { it.word.addedAt }
        return ordered.take(size)
    }
}
