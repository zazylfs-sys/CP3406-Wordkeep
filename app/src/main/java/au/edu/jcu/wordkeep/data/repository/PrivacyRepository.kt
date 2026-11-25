package au.edu.jcu.wordkeep.data.repository

import au.edu.jcu.wordkeep.data.local.WordkeepDatabase
import au.edu.jcu.wordkeep.data.local.toStudyWord
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** The learner's control over their own data: take a copy of it, or erase it. */
interface PrivacyRepository {
    /** Writes saved words and progress as CSV. Returns the number of words written. */
    suspend fun exportCsv(output: OutputStream): Int

    /** Deletes every word, answer and setting. The app returns to its first-run state. */
    suspend fun deleteEverything()
}

@Singleton
class RoomPrivacyRepository @Inject constructor(
    private val database: WordkeepDatabase,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PrivacyRepository {

    override suspend fun exportCsv(output: OutputStream): Int = withContext(io) {
        val words = database.wordDao().getAll().map { it.toStudyWord() }
        val answers = database.reviewLogDao().getAll().groupBy { it.wordId }
        output.bufferedWriter().use { writer ->
            writer.appendLine(
                "word,part_of_speech,definition,status,interval_days,next_review,lapses,answers,correct_answers,note"
            )
            words.forEach { (word, review) ->
                val logs = answers[word.id].orEmpty()
                val row = listOf(
                    word.text,
                    word.primarySense?.partOfSpeech.orEmpty(),
                    word.primarySense?.definition.orEmpty(),
                    review.mastery.label,
                    review.intervalDays.toString(),
                    review.dueDate?.toString().orEmpty(),
                    review.lapses.toString(),
                    logs.size.toString(),
                    logs.count { it.correct }.toString(),
                    word.note.orEmpty(),
                )
                writer.appendLine(row.joinToString(",") { csvField(it) })
            }
        }
        words.size
    }

    override suspend fun deleteEverything() = withContext(io) {
        database.clearAllTables()
        settingsRepository.clear()
    }

    companion object {
        /** RFC 4180 quoting: wrap in quotes when needed and double any embedded quotes. */
        fun csvField(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else {
                value
            }
    }
}
