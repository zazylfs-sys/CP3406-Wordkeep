package au.edu.jcu.wordkeep.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import au.edu.jcu.wordkeep.data.local.dao.CardDao
import au.edu.jcu.wordkeep.data.local.dao.LookAlikeDao
import au.edu.jcu.wordkeep.data.local.dao.ReviewLogDao
import au.edu.jcu.wordkeep.data.local.dao.WordDao
import au.edu.jcu.wordkeep.data.local.entity.CardEntity
import au.edu.jcu.wordkeep.data.local.entity.LookAlikeEntity
import au.edu.jcu.wordkeep.data.local.entity.ReviewLogEntity
import au.edu.jcu.wordkeep.data.local.entity.SenseEntity
import au.edu.jcu.wordkeep.data.local.entity.WordEntity

/**
 * Version history:
 * 1. words, senses, cards, review_log.
 * 2. adds words.note (learner's memory hook) and the look_alikes cache. Both are additive,
 *    so Room generates the migration and existing progress is kept.
 */
@Database(
    entities = [
        WordEntity::class,
        SenseEntity::class,
        CardEntity::class,
        ReviewLogEntity::class,
        LookAlikeEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class WordkeepDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun lookAlikeDao(): LookAlikeDao

    companion object {
        const val NAME = "wordkeep.db"
    }
}
