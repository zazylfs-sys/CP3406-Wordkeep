package au.edu.jcu.wordkeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import au.edu.jcu.wordkeep.data.local.dao.CardDao
import au.edu.jcu.wordkeep.data.local.dao.ReviewLogDao
import au.edu.jcu.wordkeep.data.local.dao.WordDao
import au.edu.jcu.wordkeep.data.local.entity.CardEntity
import au.edu.jcu.wordkeep.data.local.entity.ReviewLogEntity
import au.edu.jcu.wordkeep.data.local.entity.SenseEntity
import au.edu.jcu.wordkeep.data.local.entity.WordEntity

@Database(
    entities = [WordEntity::class, SenseEntity::class, CardEntity::class, ReviewLogEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WordkeepDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        const val NAME = "wordkeep.db"
    }
}
