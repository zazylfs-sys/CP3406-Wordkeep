package au.edu.jcu.wordkeep.ui.session

import androidx.lifecycle.viewModelScope
import au.edu.jcu.wordkeep.data.audio.FeedbackPlayer
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.audio.Pronouncer
import au.edu.jcu.wordkeep.data.repository.AnswerRecord
import au.edu.jcu.wordkeep.data.repository.InMemorySettingsRepository
import au.edu.jcu.wordkeep.data.repository.StudyRepository
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.domain.model.Accent
import au.edu.jcu.wordkeep.domain.model.DictionaryEntry
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.ReviewState
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.spelling.SpellingVerdict
import au.edu.jcu.wordkeep.domain.study.Sm2Scheduler
import au.edu.jcu.wordkeep.domain.testWord
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.now(clock)
    private lateinit var study: RecordingStudyRepository
    private lateinit var model: SessionViewModel

    @After
    fun cancelViewModelScope() {
        if (::model.isInitialized) model.viewModelScope.cancel()
    }

    @Test
    fun `flashcards contain only due words and the remaining daily allowance of new words`() = runTest {
        val due = studiedWord(1, "concept")
        val newFirst = newWord(2, "theory")
        val newSecond = newWord(3, "sample")
        val alreadyIntroduced = studiedWord(4, "result", today.plusDays(1)).let {
            it.copy(review = it.review.copy(introducedOn = today))
        }
        val pending = newWord(5, "analysis").let { it.copy(word = it.word.copy(fetchState = FetchState.PENDING)) }
        start(StudyMode.FLASHCARDS, listOf(newSecond, alreadyIntroduced, pending, newFirst, due), UserSettings(dailyNewWords = 2))
        runCurrent()

        assertEquals(2, active().total)
        assertEquals(due, active().item.word)
        gradeFlashcard(Grade.GOOD)
        assertEquals(newFirst, active().item.word)
        gradeFlashcard(Grade.GOOD)

        assertEquals(2, summary().answered)
        runCurrent()
        assertEquals(listOf(1L, 2L), study.answers.map { it.wordId })
    }

    @Test
    fun `again queues one repeat and the repeat does not reschedule the card`() = runTest {
        val due = studiedWord(1, "concept")
        val next = studiedWord(2, "sample")
        start(StudyMode.FLASHCARDS, listOf(due, next))
        runCurrent()

        assertFalse((active().item as SessionItem.Flashcard).isRepeat)
        gradeFlashcard(Grade.AGAIN)
        assertEquals(next, active().item.word)
        assertEquals(3, active().total)
        gradeFlashcard(Grade.GOOD)
        val repeat = active().item as SessionItem.Flashcard
        assertTrue(repeat.isRepeat)
        assertEquals(due, repeat.word)
        gradeFlashcard(Grade.AGAIN)
        runCurrent()

        assertTrue(model.uiState.value is SessionUiState.Finished)
        assertEquals(listOf(1L, 2L, 1L), study.answers.map { it.wordId })
        assertEquals(Sm2Scheduler.schedule(due.review, Grade.AGAIN, today), study.answers.first().newState)
        assertEquals(AnswerRecord(1L, StudyMode.FLASHCARDS, null, false, 0L, null), study.answers.last())
    }

    @Test
    fun `summary counts first answers and keeps missed ids after a successful repeat`() = runTest {
        start(StudyMode.FLASHCARDS, listOf(studiedWord(1, "concept"), studiedWord(2, "sample")))
        runCurrent()

        gradeFlashcard(Grade.AGAIN)
        gradeFlashcard(Grade.GOOD)
        gradeFlashcard(Grade.GOOD)
        runCurrent()

        assertEquals(listOf(false, true, true), study.answers.map { it.correct })
        assertEquals(SessionSummary(answered = 2, correct = 1, seconds = 0, missedWordIds = listOf(1L)), summary())
    }

    @Test
    fun `meaning match only reschedules a word that is due`() = runTest {
        val due = studiedWord(1, "concept")
        val future = studiedWord(2, "sample", today.plusDays(1))
        start(StudyMode.MEANING_MATCH, listOf(future, newWord(3, "theory"), due))
        runCurrent()

        val first = active().item as SessionItem.Meaning
        assertEquals(due, first.word)
        model.choose(first.question.answerIndex)
        model.next()
        val second = active().item as SessionItem.Meaning
        assertEquals(future, second.word)
        model.choose(second.question.answerIndex)
        runCurrent()

        val (dueAnswer, futureAnswer) = study.answers.also { assertEquals(2, it.size) }
        assertEquals(1L, dueAnswer.wordId)
        assertEquals(StudyMode.MEANING_MATCH, dueAnswer.mode)
        assertTrue(dueAnswer.correct)
        assertEquals(Grade.GOOD, dueAnswer.grade)
        assertEquals(Sm2Scheduler.schedule(due.review, Grade.GOOD, today), dueAnswer.newState)
        assertEquals(AnswerRecord(2L, StudyMode.MEANING_MATCH, null, true, 0L, null), futureAnswer)
    }

    @Test
    fun `spelling near miss allows a correct second attempt graded HARD`() = runTest {
        val due = studiedWord(1, "concept")
        start(StudyMode.LISTEN_AND_SPELL, listOf(due))
        runCurrent()

        submit("concep")
        runCurrent()
        val first = active().item as SessionItem.Spelling
        assertTrue(first.canRetry)
        assertFalse(first.finished)
        assertEquals(1, first.attemptsUsed)
        assertTrue(study.answers.isEmpty())

        submit("concept")
        runCurrent()
        val second = active().item as SessionItem.Spelling
        assertEquals(SpellingVerdict.Correct(usedAlternate = false), second.verdict)
        assertTrue(second.finished)
        assertFalse(second.canRetry)
        assertEquals(2, second.attemptsUsed)
        val answer = study.answers.single()
        assertTrue(answer.correct)
        assertEquals(Grade.HARD, answer.grade)
        assertEquals(Sm2Scheduler.schedule(due.review, Grade.HARD, today), answer.newState)
    }

    @Test
    fun `a spelling fixed on the second attempt does not count as correct first time`() = runTest {
        start(StudyMode.LISTEN_AND_SPELL, listOf(studiedWord(1, "concept")))
        runCurrent()

        submit("concep")
        submit("concept")
        runCurrent()
        model.next()
        runCurrent()

        assertEquals(0, summary().correct)
        assertEquals(listOf(1L), summary().missedWordIds)
    }

    @Test
    fun `meaning match never plays the answer automatically`() = runTest {
        val pronouncer = CountingPronouncer()
        val words = listOf(studiedWord(1, "concept"), studiedWord(2, "theory"), studiedWord(3, "method"))
        start(StudyMode.MEANING_MATCH, words, UserSettings(autoPlayPronunciation = true), pronouncer)
        runCurrent()

        assertTrue(active().item is SessionItem.Meaning)
        assertEquals(0, pronouncer.calls)
    }

    @Test
    fun `repeat flashcards show no interval because they do not reschedule`() = runTest {
        start(StudyMode.FLASHCARDS, listOf(studiedWord(1, "concept")))
        runCurrent()
        model.reveal()
        model.grade(Grade.AGAIN)
        runCurrent()

        val repeat = active().item as SessionItem.Flashcard
        assertTrue(repeat.isRepeat)
        assertTrue(repeat.intervals.isEmpty())
    }

    @Test
    fun `two spelling near misses end the item and block a third attempt`() = runTest {
        start(StudyMode.LISTEN_AND_SPELL, listOf(studiedWord(1, "concept")))
        runCurrent()

        submit("concep")
        submit("consept")
        val finished = active().item as SessionItem.Spelling
        assertTrue(finished.verdict is SpellingVerdict.NearMiss)
        assertTrue(finished.finished)
        assertFalse(finished.canRetry)
        assertEquals(2, finished.attemptsUsed)
        submit("concept")
        runCurrent()

        assertEquals(finished, active().item)
        assertEquals(Grade.AGAIN, study.answers.single().grade)
    }

    @Test
    fun `giving up on spelling records AGAIN for the due word`() = runTest {
        val due = studiedWord(1, "concept")
        start(StudyMode.LISTEN_AND_SPELL, listOf(due))
        runCurrent()

        model.giveUpSpelling()
        runCurrent()

        val item = active().item as SessionItem.Spelling
        assertTrue(item.finished)
        assertTrue(item.verdict is SpellingVerdict.Incorrect)
        val answer = study.answers.single()
        assertFalse(answer.correct)
        assertEquals(Grade.AGAIN, answer.grade)
        assertEquals(Sm2Scheduler.schedule(due.review, Grade.AGAIN, today), answer.newState)
    }

    private fun start(
        mode: StudyMode,
        words: List<StudyWord>,
        settings: UserSettings = UserSettings(),
        pronouncer: Pronouncer = SilentPronouncer(),
    ) {
        study = RecordingStudyRepository(words)
        model = SessionViewModel(
            mode,
            study,
            SessionWordRepository(),
            InMemorySettingsRepository(settings),
            pronouncer,
            SilentFeedback(),
            clock,
            Random(1),
            // Answers are saved in the app scope; here it runs on the test's main dispatcher.
            CoroutineScope(Dispatchers.Main),
        )
        assertEquals(SessionUiState.Loading(mode), model.uiState.value)
    }

    private fun newWord(id: Long, text: String) =
        StudyWord(testWord(id, text).copy(addedAt = clock.instant().plusSeconds(id)), ReviewState())

    private fun studiedWord(id: Long, text: String, due: LocalDate = today) =
        newWord(id, text).copy(review = ReviewState(repetitions = 1, intervalDays = 3, dueDate = due))

    private fun active() = model.uiState.value as SessionUiState.Active

    private fun summary() = (model.uiState.value as SessionUiState.Finished).summary

    private fun gradeFlashcard(grade: Grade) {
        model.reveal()
        model.grade(grade)
    }

    private fun submit(attempt: String) {
        model.updateAttempt(attempt)
        model.submitSpelling()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(StandardTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

private class RecordingStudyRepository(private val words: List<StudyWord>) : StudyRepository {
    val answers = mutableListOf<AnswerRecord>()

    override suspend fun studyWords() = words
    override fun observeDueCount(today: LocalDate) = flowOf(0)
    override fun observeIntroducedToday(today: LocalDate) = flowOf(0)
    override fun observeAnsweredToday(today: LocalDate) = flowOf(0)

    override suspend fun recordAnswer(answer: AnswerRecord) {
        answers += answer
    }
}

private class SessionWordRepository : WordRepository {
    override suspend fun lookAlikes(word: Word) = emptyList<String>()
    override fun observeLibrary() = error("Session must load words through StudyRepository")
    override fun observeWord(id: Long) = error("Unexpected observeWord")
    override fun observePendingCount() = error("Unexpected observePendingCount")
    override suspend fun lookUp(text: String) = error("Unexpected lookUp")
    override suspend fun save(entry: DictionaryEntry) = error("Unexpected save")
    override suspend fun addStarterPack() = error("Unexpected addStarterPack")
    override suspend fun downloadPending(onProgress: (Int, Int) -> Unit) = error("Unexpected downloadPending")
    override suspend fun updateNote(id: Long, note: String?) = error("Unexpected updateNote")
    override suspend fun delete(id: Long) = error("Unexpected delete")
    override suspend fun resetProgress(id: Long) = error("Unexpected resetProgress")
}

private class SilentPronouncer : Pronouncer {
    override val state = MutableStateFlow(PlaybackState.IDLE)
    override fun pronounce(word: Word, accent: Accent, allowNetwork: Boolean) = Unit
    override fun stop() = Unit
}

private class CountingPronouncer : Pronouncer {
    var calls = 0
    override val state = MutableStateFlow(PlaybackState.IDLE)
    override fun pronounce(word: Word, accent: Accent, allowNetwork: Boolean) {
        calls++
    }
    override fun stop() = Unit
}

private class SilentFeedback : FeedbackPlayer {
    override fun correct() = Unit
    override fun incorrect() = Unit
}
