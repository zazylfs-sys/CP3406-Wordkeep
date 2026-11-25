package au.edu.jcu.wordkeep.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import au.edu.jcu.wordkeep.data.audio.FeedbackPlayer
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.audio.Pronouncer
import au.edu.jcu.wordkeep.data.repository.AnswerRecord
import au.edu.jcu.wordkeep.data.repository.StudyRepository
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.quiz.MeaningQuizBuilder
import au.edu.jcu.wordkeep.domain.spelling.SpellingJudge
import au.edu.jcu.wordkeep.domain.spelling.SpellingVerdict
import au.edu.jcu.wordkeep.domain.study.SessionPlanner
import au.edu.jcu.wordkeep.domain.study.Sm2Scheduler
import au.edu.jcu.wordkeep.ui.navigation.SessionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

/**
 * Runs one finite study session in any of the three modes.
 *
 * Scheduling rule: flashcard grades always reschedule the word (that is what the review is for).
 * Quiz and spelling answers reschedule a word only when it is already due, so extra practice can
 * never push a word further away than the schedule intended. Every answer is logged for statistics.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studyRepository: StudyRepository,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val pronouncer: Pronouncer,
    private val feedback: FeedbackPlayer,
    private val clock: Clock,
    private val random: Random,
) : ViewModel() {

    private val mode: StudyMode = savedStateHandle.toRoute<SessionRoute>().mode
    private val today: LocalDate = LocalDate.now(clock)

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading(mode))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val playback: StateFlow<PlaybackState> = pronouncer.state

    val settings: StateFlow<UserSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val queue = mutableListOf<SessionItem>()
    private var index = 0
    private val startedAt = clock.millis()
    private var itemShownAt = startedAt
    /** First answer per word, for the summary. Repeats of "Again" cards do not count twice. */
    private val firstResults = linkedMapOf<Long, Boolean>()
    private val repeatedWords = mutableSetOf<Long>()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val prefs = settingsRepository.settings.first()
        val words = studyRepository.studyWords()
        val ready = words.filter { it.word.isReady }

        val items: List<SessionItem> = when (mode) {
            StudyMode.FLASHCARDS ->
                SessionPlanner.plan(words, today, prefs.dailyNewWords, prefs.sessionLength).map(::flashcardFor)
            StudyMode.MEANING_MATCH -> meaningItems(ready, prefs.sessionLength)
            StudyMode.LISTEN_AND_SPELL ->
                SessionPlanner.quizPool(words, today, prefs.sessionLength).map { SessionItem.Spelling(it) }
        }

        if (items.isEmpty()) {
            val reason = when {
                ready.isEmpty() -> EmptyReason.NO_WORDS
                mode == StudyMode.FLASHCARDS -> EmptyReason.NOTHING_DUE
                else -> EmptyReason.NOT_ENOUGH_WORDS
            }
            val next = ready.mapNotNull { it.review.dueDate }.filter { it.isAfter(today) }.minOrNull()
            _uiState.value = SessionUiState.Empty(mode, reason, next, today)
            return
        }
        queue += items
        show(0)
    }

    private suspend fun meaningItems(ready: List<StudyWord>, size: Int): List<SessionItem> {
        val pool = SessionPlanner.quizPool(ready, today, size)
        val builder = MeaningQuizBuilder(random)
        val deck = ready.map { it.word }
        // Fetch look-alikes for all words at once; each call is cached in Room after the first time.
        val lookAlikes = coroutineScope {
            pool.map { async { it.word.id to wordRepository.lookAlikes(it.word) } }.awaitAll().toMap()
        }
        return pool.mapNotNull { studyWord ->
            builder.build(studyWord.word, deck, lookAlikes[studyWord.word.id].orEmpty())
                ?.let { SessionItem.Meaning(studyWord, it) }
        }
    }

    private fun flashcardFor(word: StudyWord, isRepeat: Boolean = false) = SessionItem.Flashcard(
        word = word,
        revealed = false,
        intervals = Sm2Scheduler.preview(word.review, today),
        isRepeat = isRepeat,
    )

    private fun show(position: Int) {
        index = position
        itemShownAt = clock.millis()
        val item = queue[position]
        _uiState.value = SessionUiState.Active(mode, position, queue.size, item)
        val prefs = settings.value
        val autoPlay = when (item) {
            is SessionItem.Spelling -> true // the activity is listening, so the word always plays
            else -> prefs.autoPlayPronunciation
        }
        if (autoPlay) pronounce()
    }

    private fun current(): SessionItem? = (_uiState.value as? SessionUiState.Active)?.item

    private fun replaceCurrent(item: SessionItem) {
        queue[index] = item
        _uiState.update { state -> (state as? SessionUiState.Active)?.copy(item = item) ?: state }
    }

    fun pronounce() {
        val word = current()?.word?.word ?: return
        val prefs = settings.value
        pronouncer.pronounce(word, prefs.accent, allowNetwork = !prefs.offlineMode)
    }

    // ---- Flashcards ----

    fun reveal() {
        val card = current() as? SessionItem.Flashcard ?: return
        if (!card.revealed) replaceCurrent(card.copy(revealed = true))
    }

    fun grade(grade: Grade) {
        val card = current() as? SessionItem.Flashcard ?: return
        if (!card.revealed) return
        val wordId = card.word.word.id
        if (card.isRepeat) {
            // A same-day repeat is practice: SM-2 already rescheduled this word on the first pass.
            record(card.word, grade, grade.isPass, reschedule = false)
        } else {
            record(card.word, grade, grade.isPass, reschedule = true)
            if (grade == Grade.AGAIN && repeatedWords.add(wordId)) {
                queue += flashcardFor(card.word, isRepeat = true)
            }
        }
        advance()
    }

    // ---- Meaning match ----

    fun choose(optionIndex: Int) {
        val item = current() as? SessionItem.Meaning ?: return
        if (item.answered) return
        val answered = item.copy(selectedIndex = optionIndex)
        replaceCurrent(answered)
        playFeedback(answered.isCorrect)
        record(item.word, if (answered.isCorrect) Grade.GOOD else Grade.AGAIN, answered.isCorrect, reschedule = isDue(item.word))
    }

    // ---- Listen & spell ----

    fun updateAttempt(text: String) {
        val item = current() as? SessionItem.Spelling ?: return
        if (!item.finished) replaceCurrent(item.copy(attempt = text))
    }

    fun toggleHint() {
        val item = current() as? SessionItem.Spelling ?: return
        replaceCurrent(item.copy(hintVisible = !item.hintVisible))
    }

    fun submitSpelling() {
        val item = current() as? SessionItem.Spelling ?: return
        if (item.finished || item.attempt.isBlank()) return
        val word = item.word.word
        val verdict = SpellingJudge.judge(item.attempt, word.text, word.alternateSpellings)
        val attempts = item.attemptsUsed + 1
        val finished = verdict is SpellingVerdict.Correct ||
            verdict is SpellingVerdict.Incorrect ||
            attempts >= MAX_SPELLING_ATTEMPTS
        replaceCurrent(item.copy(verdict = verdict, attemptsUsed = attempts, finished = finished))
        if (finished) {
            val correct = verdict is SpellingVerdict.Correct
            playFeedback(correct)
            val grade = when {
                !correct -> Grade.AGAIN
                attempts == 1 -> Grade.GOOD
                else -> Grade.HARD
            }
            record(item.word, grade, correct, reschedule = isDue(item.word))
        }
    }

    /** "Show me": ends the item as incorrect and reveals the spelling. */
    fun giveUpSpelling() {
        val item = current() as? SessionItem.Spelling ?: return
        if (item.finished) return
        val word = item.word.word
        val verdict = SpellingVerdict.Incorrect(SpellingJudge.diff(item.attempt.trim().lowercase(), word.text.lowercase()))
        replaceCurrent(item.copy(verdict = verdict, finished = true, attemptsUsed = item.attemptsUsed + 1))
        record(item.word, Grade.AGAIN, correct = false, reschedule = isDue(item.word))
    }

    // ---- Shared ----

    fun next() {
        val item = current() ?: return
        val done = when (item) {
            is SessionItem.Meaning -> item.answered
            is SessionItem.Spelling -> item.finished
            is SessionItem.Flashcard -> false // flashcards advance by grading
        }
        if (done) advance()
    }

    fun stopAudio() = pronouncer.stop()

    private fun advance() {
        pronouncer.stop()
        if (index + 1 < queue.size) {
            show(index + 1)
        } else {
            _uiState.value = SessionUiState.Finished(
                mode,
                SessionSummary(
                    answered = firstResults.size,
                    correct = firstResults.values.count { it },
                    seconds = ((clock.millis() - startedAt) / 1000).toInt(),
                    missedWordIds = firstResults.filterValues { !it }.keys.toList(),
                ),
            )
        }
    }

    private fun isDue(word: StudyWord) = word.review.isDue(today)

    private fun record(word: StudyWord, grade: Grade, correct: Boolean, reschedule: Boolean) {
        val wordId = word.word.id
        firstResults.putIfAbsent(wordId, correct)
        val newState = if (reschedule) Sm2Scheduler.schedule(word.review, grade, today) else null
        val elapsed = clock.millis() - itemShownAt
        viewModelScope.launch {
            studyRepository.recordAnswer(
                AnswerRecord(
                    wordId = wordId,
                    mode = mode,
                    grade = if (reschedule) grade else null,
                    correct = correct,
                    responseMillis = elapsed,
                    newState = newState,
                )
            )
        }
    }

    private fun playFeedback(correct: Boolean) {
        if (!settings.value.soundEffects) return
        if (correct) feedback.correct() else feedback.incorrect()
    }

    override fun onCleared() {
        pronouncer.stop()
    }

    companion object {
        const val MAX_SPELLING_ATTEMPTS = 2
    }
}
