package au.edu.jcu.wordkeep.domain.study

import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.ReviewState
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.testWord
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPlannerTest {
    private val today = LocalDate.of(2026, 1, 31)

    @Test
    fun `due words come before new words ordered by oldest due date then lowest ease`() {
        val fresh = studyWord(1)
        val easier = studyWord(2, ReviewState(dueDate = today.minusDays(1), easeFactor = 2.6))
        val harder = studyWord(3, ReviewState(dueDate = today.minusDays(1), easeFactor = 1.5))
        val oldest = studyWord(4, ReviewState(dueDate = today.minusDays(5), easeFactor = 2.8))
        val dueToday = studyWord(5, ReviewState(dueDate = today, easeFactor = 1.3))
        val future = studyWord(6, ReviewState(dueDate = today.plusDays(1)))

        val plan = SessionPlanner.plan(
            listOf(fresh, dueToday, easier, oldest, future, harder), today, newWordsPerDay = 1, maxSize = 10,
        )

        assertEquals(listOf(oldest, harder, easier, dueToday, fresh), plan)
    }

    @Test
    fun `only words introduced today consume the new word allowance`() {
        val introducedToday = studyWord(1, ReviewState(dueDate = today.plusDays(1), introducedOn = today))
        val introducedYesterday = studyWord(2, ReviewState(dueDate = today, introducedOn = today.minusDays(1)))
        val older = studyWord(4)
        val newer = studyWord(3, addedAt = Instant.EPOCH.plusSeconds(60))

        val plan = SessionPlanner.plan(
            listOf(newer, introducedToday, older, introducedYesterday), today, newWordsPerDay = 2, maxSize = 10,
        )

        assertEquals(listOf(introducedYesterday, older), plan)
    }

    @Test
    fun `exhausted new word allowance still permits due reviews`() {
        val due = studyWord(1, ReviewState(dueDate = today, introducedOn = today.minusDays(1)))
        val introducedToday = ReviewState(dueDate = today.plusDays(1), introducedOn = today)
        val words = listOf(studyWord(2), due, studyWord(3, introducedToday), studyWord(4, introducedToday))

        assertEquals(listOf(due), SessionPlanner.plan(words, today, newWordsPerDay = 1, maxSize = 10))
    }

    @Test
    fun `pending and not found words and words without senses are excluded`() {
        val ready = studyWord(1)
        val due = ReviewState(dueDate = today)
        val pending = StudyWord(testWord(2, "pending").copy(fetchState = FetchState.PENDING), due)
        val notFound = StudyWord(testWord(3, "absent").copy(fetchState = FetchState.NOT_FOUND), ReviewState())
        val noSenses = StudyWord(testWord(4, "empty").copy(senses = emptyList()), due)
        val words = listOf(pending, notFound, noSenses, ready)

        assertEquals(listOf(ready), SessionPlanner.plan(words, today, newWordsPerDay = 5, maxSize = 10))
        assertEquals(listOf(ready), SessionPlanner.quizPool(words, today, size = 10))
    }

    @Test
    fun `maximum session size applies across due and new words`() {
        val due = studyWord(1, ReviewState(dueDate = today))
        val older = studyWord(3)
        val newer = studyWord(2, addedAt = Instant.EPOCH.plusSeconds(60))
        val words = listOf(newer, older, due)

        assertEquals(listOf(due, older), SessionPlanner.plan(words, today, newWordsPerDay = 5, maxSize = 2))
        assertEquals(emptyList<StudyWord>(), SessionPlanner.plan(words, today, newWordsPerDay = 5, maxSize = 0))
    }

    @Test
    fun `quiz pool orders due words first then shorter intervals and lower ease before new words`() {
        val due = studyWord(1, ReviewState(dueDate = today, intervalDays = 6, easeFactor = 1.5))
        val easierDue = studyWord(2, ReviewState(dueDate = today, intervalDays = 6, easeFactor = 2.5))
        val shorterDue = studyWord(3, ReviewState(dueDate = today, intervalDays = 2, easeFactor = 2.8))
        val future = studyWord(4, ReviewState(dueDate = today.plusDays(1), intervalDays = 1))
        val older = studyWord(6)
        val newer = studyWord(5, addedAt = Instant.EPOCH.plusSeconds(60))
        val words = listOf(newer, future, easierDue, older, due, shorterDue)

        val ordered = listOf(shorterDue, due, easierDue, future, older, newer)
        assertEquals(ordered, SessionPlanner.quizPool(words, today, size = 10))
        assertEquals(ordered.take(3), SessionPlanner.quizPool(words, today, size = 3))
    }

    @Test
    fun `quiz pool includes a ready word when none have been studied`() {
        val ready = studyWord(1)

        assertEquals(listOf(ready), SessionPlanner.quizPool(listOf(ready), today, size = 1))
    }

    private fun studyWord(id: Long, review: ReviewState = ReviewState(), addedAt: Instant = Instant.EPOCH) =
        StudyWord(testWord(id, "word$id").copy(addedAt = addedAt), review)
}
