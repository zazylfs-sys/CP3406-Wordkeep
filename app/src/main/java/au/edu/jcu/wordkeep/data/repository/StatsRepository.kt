package au.edu.jcu.wordkeep.data.repository

import au.edu.jcu.wordkeep.data.local.dao.CardDao
import au.edu.jcu.wordkeep.data.local.dao.ReviewLogDao
import au.edu.jcu.wordkeep.data.local.toReviewState
import au.edu.jcu.wordkeep.domain.stats.DayActivity
import au.edu.jcu.wordkeep.domain.stats.HardWord
import au.edu.jcu.wordkeep.domain.stats.LearningStats
import au.edu.jcu.wordkeep.domain.stats.ModeAccuracy
import au.edu.jcu.wordkeep.domain.stats.StatsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface StatsRepository {
    fun observeStats(today: LocalDate): Flow<LearningStats>
}

/**
 * Combines several observable Room queries into one [LearningStats]. Aggregation that SQL does
 * well (group by day, by mode, hardest words) runs in the database; the rest is in
 * [StatsCalculator] so it can be unit tested.
 */
@Singleton
class RoomStatsRepository @Inject constructor(
    private val cardDao: CardDao,
    private val logDao: ReviewLogDao,
) : StatsRepository {

    override fun observeStats(today: LocalDate): Flow<LearningStats> {
        val historyStart = today.minusDays(StatsCalculator.HISTORY_DAYS - 1L)
        val activeStart = today.minusDays(StatsCalculator.ACTIVE_WINDOW_DAYS - 1L)

        val history = combine(
            logDao.observeDailyTotals(historyStart),
            logDao.observeModeTotals(),
            logDao.observeHardestWords(minAttempts = 2, limit = 5),
        ) { days, modes, hardest -> Triple(days, modes, hardest) }

        return combine(
            cardDao.observeAll(),
            history,
            logDao.observeTotals(),
            logDao.observeStudyDays(activeStart),
        ) { cards, (days, modes, hardest), totals, studyDays ->
            val states = cards.map { it.toReviewState() }
            LearningStats(
                mastery = StatsCalculator.mastery(states),
                lastTwoWeeks = StatsCalculator.fillDays(
                    days.map { DayActivity(it.day, it.answered, it.correct) },
                    today,
                ),
                forecast = StatsCalculator.forecast(states, today),
                byMode = modes.map { ModeAccuracy(it.mode, it.answered, it.correct) },
                hardestWords = hardest.map { HardWord(it.wordId, it.text, it.attempts, it.correct, it.lapses) },
                totalAnswered = totals.answered,
                totalCorrect = totals.correct,
                studyMinutes = (totals.millis / 60_000L).toInt(),
                activeDaysLast30 = StatsCalculator.activeDays(studyDays, today),
            )
        }
    }
}
