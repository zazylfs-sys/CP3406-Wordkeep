package au.edu.jcu.wordkeep.domain.stats

import au.edu.jcu.wordkeep.domain.model.MasteryLevel
import au.edu.jcu.wordkeep.domain.model.ReviewState
import au.edu.jcu.wordkeep.domain.model.StudyMode
import java.time.LocalDate

/** Answers given on one calendar day. */
data class DayActivity(val date: LocalDate, val answered: Int, val correct: Int) {
    val accuracyPercent: Int? get() = StatsCalculator.percent(correct, answered)
}

data class ModeAccuracy(val mode: StudyMode, val answered: Int, val correct: Int) {
    val accuracyPercent: Int? get() = StatsCalculator.percent(correct, answered)
}

data class DueForecast(val date: LocalDate, val count: Int)

data class MasteryBreakdown(val new: Int, val learning: Int, val mastered: Int) {
    val total: Int get() = new + learning + mastered
    fun count(level: MasteryLevel): Int = when (level) {
        MasteryLevel.NEW -> new
        MasteryLevel.LEARNING -> learning
        MasteryLevel.MASTERED -> mastered
    }
}

data class HardWord(val wordId: Long, val text: String, val attempts: Int, val correct: Int, val lapses: Int) {
    val accuracyPercent: Int get() = StatsCalculator.percent(correct, attempts) ?: 0
}

/** Everything the statistics screen shows, computed from Room data. */
data class LearningStats(
    val mastery: MasteryBreakdown,
    val lastTwoWeeks: List<DayActivity>,
    val forecast: List<DueForecast>,
    val byMode: List<ModeAccuracy>,
    val hardestWords: List<HardWord>,
    val totalAnswered: Int,
    val totalCorrect: Int,
    val studyMinutes: Int,
    val activeDaysLast30: Int,
) {
    val overallAccuracy: Int? get() = StatsCalculator.percent(totalCorrect, totalAnswered)
    val isEmpty: Boolean get() = totalAnswered == 0 && mastery.total == 0
}

/** Pure functions behind the statistics screen. Kept free of Android types so they are unit tested. */
object StatsCalculator {

    const val HISTORY_DAYS = 14
    const val FORECAST_DAYS = 7
    const val ACTIVE_WINDOW_DAYS = 30

    /** Rounded percentage, or `null` when there is nothing to divide (shown as "—", not 0%). */
    fun percent(part: Int, whole: Int): Int? =
        if (whole <= 0) null else Math.round(part * 100.0 / whole).toInt()

    fun mastery(states: List<ReviewState>): MasteryBreakdown {
        val counts = states.groupingBy { it.mastery }.eachCount()
        return MasteryBreakdown(
            new = counts[MasteryLevel.NEW] ?: 0,
            learning = counts[MasteryLevel.LEARNING] ?: 0,
            mastered = counts[MasteryLevel.MASTERED] ?: 0,
        )
    }

    /** One entry per day for the last [days] days, oldest first; days with no study are zero. */
    fun fillDays(activity: List<DayActivity>, today: LocalDate, days: Int = HISTORY_DAYS): List<DayActivity> {
        val byDate = activity.associateBy { it.date }
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            byDate[date] ?: DayActivity(date, 0, 0)
        }
    }

    /** Reviews due on each of the next [days] days. Overdue words are counted as due today. */
    fun forecast(states: List<ReviewState>, today: LocalDate, days: Int = FORECAST_DAYS): List<DueForecast> {
        val lastDay = today.plusDays(days - 1L)
        val counts = states
            .mapNotNull { it.dueDate }
            .map { if (it.isBefore(today)) today else it }
            .filter { !it.isAfter(lastDay) }
            .groupingBy { it }
            .eachCount()
        return (0 until days).map { offset ->
            val date = today.plusDays(offset.toLong())
            DueForecast(date, counts[date] ?: 0)
        }
    }

    /** How many different days the learner studied in the window. Deliberately not a "streak". */
    fun activeDays(studyDates: List<LocalDate>, today: LocalDate, window: Int = ACTIVE_WINDOW_DAYS): Int {
        val first = today.minusDays(window - 1L)
        return studyDates.filter { !it.isBefore(first) && !it.isAfter(today) }.distinct().size
    }
}
