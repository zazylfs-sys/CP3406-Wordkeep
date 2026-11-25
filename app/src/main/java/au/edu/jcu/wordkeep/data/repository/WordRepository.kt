package au.edu.jcu.wordkeep.data.repository

import au.edu.jcu.wordkeep.domain.model.DictionaryEntry
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.model.Word
import kotlinx.coroutines.flow.Flow

/** Saved words and dictionary look-ups. Room is the source of truth; the network fills it. */
interface WordRepository {
    fun observeLibrary(): Flow<List<StudyWord>>
    fun observeWord(id: Long): Flow<StudyWord?>
    fun observePendingCount(): Flow<Int>

    /** Looks a word up, using the saved copy when there is one. Nothing is saved. */
    suspend fun lookUp(text: String): LookupResult

    /** Saves a looked-up word for study. Returns its id (the existing id if already saved). */
    suspend fun save(entry: DictionaryEntry): Long

    /** Adds the starter word list as placeholders. Returns how many new words were added. */
    suspend fun addStarterPack(): Int

    /** Downloads definitions for words added without them (the starter pack). */
    suspend fun downloadPending(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): DownloadSummary

    suspend fun updateNote(id: Long, note: String?)
    suspend fun delete(id: Long)
    suspend fun resetProgress(id: Long)

    /** Look-alike words for quiz distractors: cached ones first, then Datamuse if allowed. */
    suspend fun lookAlikes(word: Word): List<String>
}

sealed interface LookupResult {
    data class Found(val entry: DictionaryEntry, val savedWordId: Long?) : LookupResult
    data class NotFound(val text: String) : LookupResult
    data class Unavailable(val reason: Reason) : LookupResult
    data object Invalid : LookupResult

    enum class Reason { OFFLINE_MODE, NO_CONNECTION, SERVER_ERROR }
}

data class DownloadSummary(
    val downloaded: Int,
    val notFound: Int,
    val remaining: Int,
    val stoppedReason: LookupResult.Reason? = null,
)
