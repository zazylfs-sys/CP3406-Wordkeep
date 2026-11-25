package au.edu.jcu.wordkeep.domain.study

import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.ReviewState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class Sm2SchedulerTest {
    private val today = LocalDate.of(2026, 1, 31)

    @Test
    fun `first and second passing reviews schedule one and six days`() {
        val first = Sm2Scheduler.schedule(ReviewState(), Grade.GOOD, today)
        assertEquals(1, first.repetitions)
        assertEquals(1, first.intervalDays)
        assertEquals(today.plusDays(1), first.dueDate)

        val secondDay = today.plusDays(1)
        val second = Sm2Scheduler.schedule(first, Grade.GOOD, secondDay)
        assertEquals(2, second.repetitions)
        assertEquals(6, second.intervalDays)
        assertEquals(secondDay.plusDays(6), second.dueDate)
    }

    @Test
    fun `later passing reviews round up the previous interval multiplied by the updated ease`() {
        val state = ReviewState(repetitions = 2, intervalDays = 6, easeFactor = 2.5, dueDate = today)

        val review = Sm2Scheduler.schedule(state, Grade.HARD, today)

        assertEquals(3, review.repetitions)
        assertEquals(2.36, review.easeFactor, 1e-9)
        assertEquals(15, review.intervalDays) // 6 x 2.36 = 14.16, rounded up
        assertEquals(today.plusDays(15), review.dueDate)
    }

    @Test
    fun `a failed review resets repetitions and schedules the next day`() {
        val state = ReviewState(repetitions = 3, intervalDays = 15, dueDate = today.minusDays(2))

        val failed = Sm2Scheduler.schedule(state, Grade.AGAIN, today)

        assertEquals(0, failed.repetitions)
        assertEquals(1, failed.intervalDays)
        assertEquals(today.plusDays(1), failed.dueDate)

        val recovered = Sm2Scheduler.schedule(failed, Grade.GOOD, today.plusDays(1))
        assertEquals(1, recovered.repetitions)
        assertEquals(1, recovered.intervalDays)
    }

    @Test
    fun `ease changes by the SM-2 amount for each grade`() {
        val state = ReviewState(easeFactor = 2.5)
        val expectedEase = mapOf(
            Grade.AGAIN to 2.18,
            Grade.HARD to 2.36,
            Grade.GOOD to 2.50,
            Grade.EASY to 2.60,
        )

        for ((grade, expected) in expectedEase) {
            assertEquals(grade.name, expected, Sm2Scheduler.schedule(state, grade, today).easeFactor, 1e-9)
        }
    }

    @Test
    fun `repeated Again grades reach the ease floor and stay there`() {
        var state = ReviewState()
        val expectedEase = listOf(2.18, 1.86, 1.54, 1.3, 1.3, 1.3)

        for ((day, expected) in expectedEase.withIndex()) {
            state = Sm2Scheduler.schedule(state, Grade.AGAIN, today.plusDays(day.toLong()))
            assertEquals(expected, state.easeFactor, 1e-9)
        }
    }

    @Test
    fun `lapses increase only when a card with a successful repetition fails`() {
        val newFailure = Sm2Scheduler.schedule(ReviewState(), Grade.AGAIN, today)
        assertEquals(0, newFailure.lapses)

        val learned = ReviewState(repetitions = 2, intervalDays = 6, dueDate = today, lapses = 2)
        assertEquals(2, Sm2Scheduler.schedule(learned, Grade.GOOD, today).lapses)

        val lapse = Sm2Scheduler.schedule(learned, Grade.AGAIN, today)
        assertEquals(3, lapse.lapses)
        assertEquals(3, Sm2Scheduler.schedule(lapse, Grade.AGAIN, today.plusDays(1)).lapses)
    }

    @Test
    fun `introduction date is set on the first attempt and preserved on later reviews`() {
        val introduced = Sm2Scheduler.schedule(ReviewState(), Grade.AGAIN, today)
        assertEquals(today, introduced.introducedOn)

        val later = Sm2Scheduler.schedule(introduced, Grade.EASY, today.plusDays(1))
        assertEquals(today, later.introducedOn)
    }

    @Test
    fun `whole-number intervals are not pushed up by floating point error`() {
        val state = ReviewState(repetitions = 2, intervalDays = 6, easeFactor = 2.5, dueDate = today)

        assertEquals(15, Sm2Scheduler.schedule(state, Grade.GOOD, today).intervalDays)
    }

    @Test
    fun `preview returns the expected interval for every grade`() {
        val state = ReviewState(repetitions = 3, intervalDays = 10, easeFactor = 2.52, dueDate = today)

        assertEquals(
            mapOf(Grade.AGAIN to 1, Grade.HARD to 24, Grade.GOOD to 26, Grade.EASY to 27),
            Sm2Scheduler.preview(state, today),
        )
    }
}
