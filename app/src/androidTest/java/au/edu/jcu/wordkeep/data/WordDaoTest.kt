package au.edu.jcu.wordkeep.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.wordkeep.data.local.WordkeepDatabase
import au.edu.jcu.wordkeep.data.local.entity.ReviewLogEntity
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.StudyMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Room behaviour that the app relies on, tested against a real in-memory SQLite database. */
@RunWith(AndroidJUnit4::class)
class WordDaoTest {

    private lateinit var db: WordkeepDatabase
    private val today = LocalDate.of(2026, 11, 25)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WordkeepDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertWithContent_createsSensesAndAnEmptyCard() = runTest {
        val id = db.wordDao().insertWithContent(wordEntity("theory"), senses("An idea", "A principle"))

        val loaded = db.wordDao().observe(id).first()
        assertNotNull(loaded)
        assertEquals(listOf("An idea", "A principle"), loaded!!.senses.sortedBy { it.position }.map { it.definition })
        // Lists containing commas survive the JSON type converter.
        assertEquals(listOf("a, b"), loaded.senses.first().synonyms)
        assertNotNull(loaded.card)
        assertNull(loaded.card!!.dueDay)
    }

    @Test
    fun insertWithContent_returnsMinusOneForDuplicateText() = runTest {
        db.wordDao().insertWithContent(wordEntity("data"), senses("Facts"))
        assertEquals(-1L, db.wordDao().insertWithContent(wordEntity("data"), senses("Again")))
    }

    @Test
    fun findByText_ignoresCase() = runTest {
        db.wordDao().insertWithContent(wordEntity("Policy"), senses("A plan"))
        assertNotNull(db.wordDao().findByText("policy"))
    }

    @Test
    fun deletingAWord_cascadesToSensesCardsAndLogs() = runTest {
        val id = db.wordDao().insertWithContent(wordEntity("sector"), senses("A part"))
        db.reviewLogDao().insert(log(id, correct = true))

        db.wordDao().delete(id)

        assertTrue(db.reviewLogDao().getAll().isEmpty())
        assertNull(db.cardDao().get(id))
        assertNull(db.wordDao().observe(id).first())
    }

    @Test
    fun dueCount_includesOverdueReadyWordsOnly() = runTest {
        val due = db.wordDao().insertWithContent(wordEntity("source"), senses("Origin"))
        val future = db.wordDao().insertWithContent(wordEntity("vary"), senses("Change"))
        val pending = db.wordDao().insertWithContent(wordEntity("role", FetchState.PENDING), emptyList())
        db.cardDao().upsert(db.cardDao().get(due)!!.copy(dueDay = today.minusDays(2), intervalDays = 1))
        db.cardDao().upsert(db.cardDao().get(future)!!.copy(dueDay = today.plusDays(3), intervalDays = 6))
        db.cardDao().upsert(db.cardDao().get(pending)!!.copy(dueDay = today, intervalDays = 1))

        assertEquals(1, db.cardDao().observeDueCount(today).first())
    }

    @Test
    fun resetCard_makesTheWordNewAgain() = runTest {
        val id = db.wordDao().insertWithContent(wordEntity("major"), senses("Important"))
        db.cardDao().upsert(db.cardDao().get(id)!!.copy(dueDay = today, intervalDays = 15, repetitions = 3, lapses = 2))

        db.cardDao().reset(id)

        val card = db.cardDao().get(id)!!
        assertNull(card.dueDay)
        assertEquals(0, card.repetitions)
        assertEquals(0, card.lapses)
    }

    @Test
    fun aggregateQueries_groupByDayAndModeAndRankHardWords() = runTest {
        val easy = db.wordDao().insertWithContent(wordEntity("area"), senses("A region"))
        val hard = db.wordDao().insertWithContent(wordEntity("constitute"), senses("Make up"))
        val logs = db.reviewLogDao()
        logs.insert(log(easy, true, today.minusDays(1), StudyMode.FLASHCARDS))
        logs.insert(log(easy, true, today, StudyMode.MEANING_MATCH))
        logs.insert(log(hard, false, today, StudyMode.MEANING_MATCH))
        logs.insert(log(hard, false, today, StudyMode.LISTEN_AND_SPELL))
        logs.insert(log(hard, true, today, StudyMode.FLASHCARDS))

        val days = logs.observeDailyTotals(today.minusDays(13)).first()
        assertEquals(listOf(today.minusDays(1), today), days.map { it.day })
        assertEquals(4, days.last().answered)
        assertEquals(2, days.last().correct)

        val meaning = logs.observeModeTotals().first().single { it.mode == StudyMode.MEANING_MATCH }
        assertEquals(2, meaning.answered)
        assertEquals(1, meaning.correct)

        // "area" was never missed, so only "constitute" is listed.
        val hardest = logs.observeHardestWords(minAttempts = 2, limit = 5).first()
        assertEquals(listOf("constitute"), hardest.map { it.text })
        assertEquals(3, hardest.single().attempts)

        val totals = logs.observeTotals().first()
        assertEquals(5, totals.answered)
        assertEquals(3, totals.correct)
        assertEquals(5_000L, totals.millis)
        assertEquals(1, logs.observeAnsweredOn(today.minusDays(1)).first())
    }

    @Test
    fun clearAllTables_erasesEverything() = runTest {
        val id = db.wordDao().insertWithContent(wordEntity("income"), senses("Money received"))
        db.reviewLogDao().insert(log(id, true))

        db.clearAllTables()

        assertTrue(db.wordDao().getAll().isEmpty())
        assertTrue(db.reviewLogDao().getAll().isEmpty())
    }

    private fun log(
        wordId: Long,
        correct: Boolean,
        day: LocalDate = today,
        mode: StudyMode = StudyMode.FLASHCARDS,
    ) = ReviewLogEntity(
        wordId = wordId,
        mode = mode,
        grade = null,
        correct = correct,
        answeredAt = Instant.ofEpochSecond(day.toEpochDay() * 86_400),
        answeredDay = day,
        responseMillis = 1_000,
    )
}
