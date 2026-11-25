package au.edu.jcu.wordkeep.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.jcu.wordkeep.data.local.WordkeepDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that upgrading from schema 1 to 2 keeps learners' words and progress. The exported
 * schemas in app/schemas are what MigrationTestHelper rebuilds version 1 from.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), WordkeepDatabase::class.java)

    @Test
    fun migrate1To2_keepsWordsAndCardsAndAddsNewStructures() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                """INSERT INTO words (id, text, phonetic, audio_urls, alternate_spellings, origin, fetch_state, added_at, fetched_at)
                   VALUES (1, 'theory', NULL, '[]', '[]', 'LEARNER', 'READY', 0, NULL)"""
            )
            execSQL(
                """INSERT INTO cards (word_id, repetitions, interval_days, ease_factor, due_day, lapses, introduced_day, last_reviewed_at)
                   VALUES (1, 3, 15, 2.36, 20000, 1, 19990, NULL)"""
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true)

        db.query("SELECT text, note FROM words").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("theory", cursor.getString(0))
            assertTrue("new column starts empty", cursor.isNull(1))
        }
        db.query("SELECT interval_days, ease_factor FROM cards WHERE word_id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(15, cursor.getInt(0))
            assertEquals(2.36, cursor.getDouble(1), 1e-9)
        }
        db.execSQL("INSERT INTO look_alikes (word_id, text) VALUES (1, 'thesis')")
        db.query("SELECT COUNT(*) FROM look_alikes").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
