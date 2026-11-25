package au.edu.jcu.wordkeep.domain.study

import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.ReviewState
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * The SuperMemo-2 spaced-repetition algorithm (P. Wozniak, 1990).
 *
 * After each answer the interval grows (1 day, 6 days, then interval x ease factor) while recall
 * succeeds, and falls back to 1 day when it fails. The ease factor drifts with answer quality
 * but never drops below [MIN_EASE], so difficult words are not scheduled into oblivion.
 */
object Sm2Scheduler {

    const val MIN_EASE = 1.3
    private const val FIRST_INTERVAL = 1
    private const val SECOND_INTERVAL = 6

    fun schedule(state: ReviewState, grade: Grade, today: LocalDate): ReviewState {
        val q = grade.quality
        val ease = (state.easeFactor + 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
            .coerceAtLeast(MIN_EASE)

        val (repetitions, interval) = if (grade.isPass) {
            val next = when (state.repetitions) {
                0 -> FIRST_INTERVAL
                1 -> SECOND_INTERVAL
                else -> (state.intervalDays * ease).roundToInt()
            }
            state.repetitions + 1 to next
        } else {
            0 to FIRST_INTERVAL
        }

        // A lapse is forgetting a word that had already been learned at least once.
        val lapses = if (!grade.isPass && state.repetitions > 0) state.lapses + 1 else state.lapses

        return state.copy(
            repetitions = repetitions,
            intervalDays = interval,
            easeFactor = ease,
            dueDate = today.plusDays(interval.toLong()),
            lapses = lapses,
            introducedOn = state.introducedOn ?: today,
        )
    }

    /** The interval each grade would give, so the grade buttons can say "Good · 6d". */
    fun preview(state: ReviewState, today: LocalDate): Map<Grade, Int> =
        Grade.entries.associateWith { schedule(state, it, today).intervalDays }
}
