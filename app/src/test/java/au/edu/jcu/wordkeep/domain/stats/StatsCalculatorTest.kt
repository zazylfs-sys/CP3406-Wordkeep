package au.edu.jcu.wordkeep.domain.stats

import au.edu.jcu.wordkeep.domain.model.ReviewState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsCalculatorTest {
    private val today = LocalDate.of(2026, 1, 31)

    @Test
    fun `percent rounds to the nearest whole percentage`() {
        assertEquals(33, StatsCalculator.percent(1, 3))
        assertEquals(67, StatsCalculator.percent(2, 3))
        assertEquals(13, StatsCalculator.percent(1, 8))
        assertEquals(0, StatsCalculator.percent(0, 3))
    }

    @Test
    fun `percent is null when the denominator is zero`() {
        assertNull(StatsCalculator.percent(0, 0))
    }

    @Test
    fun `mastery treats a null due date as new and changes from learning to mastered at day 21`() {
        val new = ReviewState(intervalDays = 21)
        val learning = ReviewState(intervalDays = 20, dueDate = today)
        val mastered = ReviewState(intervalDays = 21, dueDate = today)

        assertEquals(MasteryBreakdown(new = 1, learning = 0, mastered = 0), StatsCalculator.mastery(listOf(new)))
        assertEquals(MasteryBreakdown(new = 0, learning = 1, mastered = 0), StatsCalculator.mastery(listOf(learning)))
        assertEquals(MasteryBreakdown(new = 0, learning = 0, mastered = 1), StatsCalculator.mastery(listOf(mastered)))
        assertEquals(
            MasteryBreakdown(new = 1, learning = 2, mastered = 1),
            StatsCalculator.mastery(listOf(new, learning, learning, mastered)),
        )
    }

    @Test
    fun `fillDays returns exactly the requested dates oldest first and fills missing activity with zeros`() {
        val oldest = DayActivity(today.minusDays(3), answered = 3, correct = 1)
        val current = DayActivity(today, answered = 5, correct = 4)
        val outside = DayActivity(today.minusDays(4), answered = 8, correct = 8)

        val filled = StatsCalculator.fillDays(listOf(current, outside, oldest), today, days = 4)

        assertEquals(
            listOf(oldest, DayActivity(today.minusDays(2), 0, 0), DayActivity(today.minusDays(1), 0, 0), current),
            filled,
        )
    }

    @Test
    fun `forecast counts overdue and current reviews today and excludes dates beyond the window`() {
        val states = listOf(
            reviewDue(-8),
            reviewDue(-1),
            reviewDue(0),
            reviewDue(2),
            reviewDue(3),
            reviewDue(4),
            ReviewState(),
        )

        val forecast = StatsCalculator.forecast(states, today, days = 4)

        assertEquals(
            listOf(
                DueForecast(today, 3),
                DueForecast(today.plusDays(1), 0),
                DueForecast(today.plusDays(2), 1),
                DueForecast(today.plusDays(3), 1),
            ),
            forecast,
        )
    }

    @Test
    fun `activeDays counts distinct study dates within the inclusive thirty-day window`() {
        val oldest = today.minusDays(29)
        val dates = listOf(today.minusDays(30), oldest, today, oldest, today.plusDays(1), today.minusDays(7), today)

        assertEquals(3, StatsCalculator.activeDays(dates, today))
    }

    private fun reviewDue(offsetDays: Long) = ReviewState(dueDate = today.plusDays(offsetDays))
}
