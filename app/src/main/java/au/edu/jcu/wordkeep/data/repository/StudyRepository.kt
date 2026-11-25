package au.edu.jcu.wordkeep.data.repository

import androidx.room.withTransaction
import au.edu.jcu.wordkeep.data.local.WordkeepDatabase
import au.edu.jcu.wordkeep.data.local.entity.ReviewLogEntity
import au.edu.jcu.wordkeep.data.local.toCardEntity
import au.edu.jcu.wordkeep.data.local.toStudyWord
import au.edu.jcu.wordkeep.di.IoDispatcher
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.ReviewState
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.model.StudyWord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Reads words for a session and records each answer. */
interface StudyRepository {
    suspend fun studyWords(): List<StudyWord>
    fun observeDueCount(today: LocalDate): Flow<Int>
    fun observeIntroducedToday(today: LocalDate): Flow<Int>
    fun observeAnsweredToday(today: LocalDate): Flow<Int>
    suspend fun recordAnswer(answer: AnswerRecord)
}

/**
 * One answer. [newState] is the rescheduled card, or `null` when the answer should be logged
 * for statistics without moving the word's schedule (extra practice on a word that is not due).
 */
data class AnswerRecord(
    val wordId: Long,
    val mode: StudyMode,
    val grade: Grade?,
    val correct: Boolean,
    val responseMillis: Long,
    val newState: ReviewState?,
)

@Singleton
class RoomStudyRepository @Inject constructor(
    private val database: WordkeepDatabase,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StudyRepository {

    private val wordDao = database.wordDao()
    private val cardDao = database.cardDao()
    private val logDao = database.reviewLogDao()

    override suspend fun studyWords(): List<StudyWord> = withContext(io) {
        wordDao.getAll().map { it.toStudyWord() }
    }

    override fun observeDueCount(today: LocalDate): Flow<Int> = cardDao.observeDueCount(today)

    override fun observeIntroducedToday(today: LocalDate): Flow<Int> = cardDao.observeIntroducedOn(today)

    override fun observeAnsweredToday(today: LocalDate): Flow<Int> = logDao.observeAnsweredOn(today)

    /** The log entry and the card update are written together, so statistics never disagree with schedules. */
    override suspend fun recordAnswer(answer: AnswerRecord): Unit = withContext(io) {
        val now = clock.instant()
        database.withTransaction {
            logDao.insert(
                ReviewLogEntity(
                    wordId = answer.wordId,
                    mode = answer.mode,
                    grade = answer.grade,
                    correct = answer.correct,
                    answeredAt = now,
                    answeredDay = LocalDate.now(clock),
                    responseMillis = answer.responseMillis,
                )
            )
            answer.newState?.let { cardDao.upsert(it.toCardEntity(answer.wordId, now)) }
        }
        Unit
    }
}
