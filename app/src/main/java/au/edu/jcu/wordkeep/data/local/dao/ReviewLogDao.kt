package au.edu.jcu.wordkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import au.edu.jcu.wordkeep.data.local.entity.DayTotalsRow
import au.edu.jcu.wordkeep.data.local.entity.HardWordRow
import au.edu.jcu.wordkeep.data.local.entity.ModeTotalsRow
import au.edu.jcu.wordkeep.data.local.entity.ReviewLogEntity
import au.edu.jcu.wordkeep.data.local.entity.TotalsRow
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Writes answers and runs the aggregate queries behind the statistics screen. */
@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLogEntity)

    @Query(
        """
        SELECT answered_day AS day, COUNT(*) AS answered, SUM(correct) AS correct
        FROM review_log WHERE answered_day >= :fromDay
        GROUP BY answered_day ORDER BY answered_day
        """
    )
    fun observeDailyTotals(fromDay: LocalDate): Flow<List<DayTotalsRow>>

    @Query("SELECT mode, COUNT(*) AS answered, SUM(correct) AS correct FROM review_log GROUP BY mode")
    fun observeModeTotals(): Flow<List<ModeTotalsRow>>

    /** Words answered at least [minAttempts] times with at least one miss, lowest accuracy first. */
    @Query(
        """
        SELECT w.id AS wordId, w.text AS text, COUNT(l.id) AS attempts, SUM(l.correct) AS correct,
               IFNULL(c.lapses, 0) AS lapses
        FROM review_log l
        JOIN words w ON w.id = l.word_id
        LEFT JOIN cards c ON c.word_id = w.id
        GROUP BY w.id
        HAVING COUNT(l.id) >= :minAttempts AND SUM(l.correct) < COUNT(l.id)
        ORDER BY CAST(SUM(l.correct) AS REAL) / COUNT(l.id) ASC, lapses DESC, attempts DESC
        LIMIT :limit
        """
    )
    fun observeHardestWords(minAttempts: Int, limit: Int): Flow<List<HardWordRow>>

    @Query(
        """
        SELECT COUNT(*) AS answered, IFNULL(SUM(correct), 0) AS correct,
               IFNULL(SUM(response_millis), 0) AS millis
        FROM review_log
        """
    )
    fun observeTotals(): Flow<TotalsRow>

    @Query("SELECT DISTINCT answered_day FROM review_log WHERE answered_day >= :fromDay")
    fun observeStudyDays(fromDay: LocalDate): Flow<List<LocalDate>>

    @Query("SELECT COUNT(*) FROM review_log WHERE answered_day = :day")
    fun observeAnsweredOn(day: LocalDate): Flow<Int>

    @Query("SELECT * FROM review_log ORDER BY answered_at")
    suspend fun getAll(): List<ReviewLogEntity>
}
