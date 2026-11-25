package au.edu.jcu.wordkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.edu.jcu.wordkeep.data.local.entity.CardEntity
import au.edu.jcu.wordkeep.data.local.entity.SenseEntity
import au.edu.jcu.wordkeep.data.local.entity.WordEntity
import au.edu.jcu.wordkeep.data.local.entity.WordWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Transaction
    @Query("SELECT * FROM words ORDER BY text COLLATE NOCASE")
    fun observeAll(): Flow<List<WordWithDetails>>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id")
    fun observe(id: Long): Flow<WordWithDetails?>

    @Transaction
    @Query("SELECT * FROM words ORDER BY added_at")
    suspend fun getAll(): List<WordWithDetails>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun get(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE text = :text COLLATE NOCASE LIMIT 1")
    suspend fun findByText(text: String): WordEntity?

    @Query("SELECT * FROM words WHERE fetch_state = 'PENDING' ORDER BY added_at")
    suspend fun getPending(): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words WHERE fetch_state = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Insert
    suspend fun insertSenses(senses: List<SenseEntity>)

    @Query("DELETE FROM senses WHERE word_id = :wordId")
    suspend fun deleteSenses(wordId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: CardEntity)

    /** Saves a new word with its senses and an empty card. Returns the id, or -1 if it already exists. */
    @Transaction
    suspend fun insertWithContent(word: WordEntity, senses: List<SenseEntity>): Long {
        val id = insert(word)
        if (id == -1L) return id
        insertSenses(senses.map { it.copy(wordId = id) })
        insertCard(CardEntity.newFor(id))
        return id
    }

    /** Replaces the downloaded dictionary content of an existing word. Progress is untouched. */
    @Transaction
    suspend fun replaceContent(word: WordEntity, senses: List<SenseEntity>) {
        update(word)
        deleteSenses(word.id)
        insertSenses(senses.map { it.copy(wordId = word.id) })
    }

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun delete(id: Long)
}
