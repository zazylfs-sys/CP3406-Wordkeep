package au.edu.jcu.wordkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import au.edu.jcu.wordkeep.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CardDao {

    @Upsert
    suspend fun upsert(card: CardEntity)

    @Query("SELECT * FROM cards WHERE word_id = :wordId")
    suspend fun get(wordId: Long): CardEntity?

    @Query("SELECT * FROM cards")
    fun observeAll(): Flow<List<CardEntity>>

    /** Words due for review today (or overdue) whose definitions are available. */
    @Query(
        """
        SELECT COUNT(*) FROM cards c JOIN words w ON w.id = c.word_id
        WHERE c.due_day IS NOT NULL AND c.due_day <= :today AND w.fetch_state = 'READY'
        """
    )
    fun observeDueCount(today: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE introduced_day = :day")
    fun observeIntroducedOn(day: LocalDate): Flow<Int>

    @Query(
        """
        UPDATE cards SET repetitions = 0, interval_days = 0, ease_factor = 2.5, due_day = NULL,
        lapses = 0, introduced_day = NULL, last_reviewed_at = NULL WHERE word_id = :wordId
        """
    )
    suspend fun reset(wordId: Long)
}
