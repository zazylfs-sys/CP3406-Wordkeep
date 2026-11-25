package au.edu.jcu.wordkeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.edu.jcu.wordkeep.data.local.entity.LookAlikeEntity

@Dao
interface LookAlikeDao {

    @Query("SELECT text FROM look_alikes WHERE word_id = :wordId")
    suspend fun forWord(wordId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<LookAlikeEntity>)
}
