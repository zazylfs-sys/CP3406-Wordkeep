package au.edu.jcu.wordkeep.data.repository

import au.edu.jcu.wordkeep.data.local.dao.CardDao
import au.edu.jcu.wordkeep.data.local.dao.LookAlikeDao
import au.edu.jcu.wordkeep.data.local.dao.WordDao
import au.edu.jcu.wordkeep.data.local.entity.LookAlikeEntity
import au.edu.jcu.wordkeep.data.local.entity.WordEntity
import au.edu.jcu.wordkeep.data.local.toDictionaryEntry
import au.edu.jcu.wordkeep.data.local.toDomain
import au.edu.jcu.wordkeep.data.local.toSenseEntities
import au.edu.jcu.wordkeep.data.local.toStudyWord
import au.edu.jcu.wordkeep.data.local.toWordEntity
import au.edu.jcu.wordkeep.data.remote.DatamuseApi
import au.edu.jcu.wordkeep.data.remote.DictionaryApi
import au.edu.jcu.wordkeep.data.remote.DictionaryMapper
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.di.IoDispatcher
import au.edu.jcu.wordkeep.domain.StarterPack
import au.edu.jcu.wordkeep.domain.model.DictionaryEntry
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import au.edu.jcu.wordkeep.domain.quiz.DistractorFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstWordRepository @Inject constructor(
    private val wordDao: WordDao,
    private val cardDao: CardDao,
    private val lookAlikeDao: LookAlikeDao,
    private val dictionaryApi: DictionaryApi,
    private val datamuseApi: DatamuseApi,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WordRepository {

    override fun observeLibrary(): Flow<List<StudyWord>> =
        wordDao.observeAll().map { rows -> rows.map { it.toStudyWord() } }

    override fun observeWord(id: Long): Flow<StudyWord?> =
        wordDao.observe(id).map { it?.toStudyWord() }

    override fun observePendingCount(): Flow<Int> = wordDao.observePendingCount()

    override suspend fun lookUp(text: String): LookupResult = withContext(io) {
        val query = normalise(text) ?: return@withContext LookupResult.Invalid

        val saved = wordDao.findByText(query)
        if (saved != null && saved.fetchState == FetchState.READY) {
            val details = wordDao.observe(saved.id).first()
            if (details != null) {
                return@withContext LookupResult.Found(details.toStudyWord().word.toDictionaryEntry(), saved.id)
            }
        }
        when (val remote = fetch(query)) {
            is Fetched.Entry -> LookupResult.Found(remote.entry, saved?.id)
            Fetched.Missing -> LookupResult.NotFound(query)
            is Fetched.Failed -> LookupResult.Unavailable(remote.reason)
        }
    }

    override suspend fun save(entry: DictionaryEntry): Long = withContext(io) {
        val existing = wordDao.findByText(entry.text)
        if (existing != null) {
            if (existing.fetchState != FetchState.READY) storeContent(existing, entry)
            return@withContext existing.id
        }
        wordDao.insertWithContent(entry.toWordEntity(WordOrigin.LEARNER, clock.instant()), entry.toSenseEntities())
    }

    override suspend fun addStarterPack(): Int = withContext(io) {
        val now = clock.instant()
        StarterPack.words.mapIndexed { index, text ->
            val placeholder = WordEntity(
                text = text,
                phonetic = null,
                audioUrls = emptyList(),
                alternateSpellings = emptyList(),
                origin = WordOrigin.STARTER_PACK,
                fetchState = FetchState.PENDING,
                // Keep the list order so new words are introduced in frequency order.
                addedAt = now.plusMillis(index.toLong()),
                fetchedAt = null,
            )
            wordDao.insertWithContent(placeholder, emptyList())
        }.count { it != -1L }
    }

    override suspend fun downloadPending(onProgress: (done: Int, total: Int) -> Unit): DownloadSummary =
        withContext(io) {
            val pending = wordDao.getPending()
            var downloaded = 0
            var notFound = 0
            pending.forEachIndexed { index, word ->
                when (val result = fetch(word.text)) {
                    is Fetched.Entry -> {
                        storeContent(word, result.entry)
                        downloaded++
                    }
                    Fetched.Missing -> {
                        wordDao.update(word.copy(fetchState = FetchState.NOT_FOUND, fetchedAt = clock.instant()))
                        notFound++
                    }
                    // Stop at the first connection problem instead of failing every remaining word.
                    is Fetched.Failed -> return@withContext DownloadSummary(
                        downloaded = downloaded,
                        notFound = notFound,
                        remaining = pending.size - index,
                        stoppedReason = result.reason,
                    )
                }
                onProgress(index + 1, pending.size)
            }
            DownloadSummary(downloaded, notFound, remaining = 0)
        }

    override suspend fun updateNote(id: Long, note: String?) = withContext(io) {
        wordDao.updateNote(id, note?.trim()?.takeIf { it.isNotEmpty() })
    }

    override suspend fun delete(id: Long) = withContext(io) { wordDao.delete(id) }

    override suspend fun resetProgress(id: Long) = withContext(io) { cardDao.reset(id) }

    override suspend fun lookAlikes(word: Word): List<String> = withContext(io) {
        val cached = lookAlikeDao.forWord(word.id)
        if (cached.isNotEmpty() || isOfflineMode()) return@withContext cached
        try {
            val candidates = datamuseApi.spelledLike(word.text).map { it.word } +
                datamuseApi.soundsLike(word.text).map { it.word }
            val usable = DistractorFilter.filter(word.text, candidates).take(MAX_LOOK_ALIKES)
            lookAlikeDao.insertAll(usable.map { LookAlikeEntity(word.id, it) })
            usable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Distractors are optional: the quiz falls back to words from the learner's deck.
            emptyList()
        }
    }

    private suspend fun storeContent(word: WordEntity, entry: DictionaryEntry) {
        val updated = word.copy(
            phonetic = entry.phonetic,
            audioUrls = entry.audioUrls,
            alternateSpellings = entry.alternateSpellings,
            fetchState = FetchState.READY,
            fetchedAt = clock.instant(),
        )
        wordDao.replaceContent(updated, entry.toSenseEntities(word.id))
    }

    private suspend fun fetch(text: String): Fetched {
        if (isOfflineMode()) return Fetched.Failed(LookupResult.Reason.OFFLINE_MODE)
        return try {
            DictionaryMapper.toEntry(text, dictionaryApi.lookUp(text))
                ?.let { Fetched.Entry(it) }
                ?: Fetched.Missing
        } catch (e: HttpException) {
            if (e.code() == 404) Fetched.Missing else Fetched.Failed(LookupResult.Reason.SERVER_ERROR)
        } catch (e: IOException) {
            Fetched.Failed(LookupResult.Reason.NO_CONNECTION)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Malformed JSON and similar: treat as a server problem rather than crashing.
            Fetched.Failed(LookupResult.Reason.SERVER_ERROR)
        }
    }

    private suspend fun isOfflineMode(): Boolean = settingsRepository.settings.first().offlineMode

    private sealed interface Fetched {
        data class Entry(val entry: DictionaryEntry) : Fetched
        data object Missing : Fetched
        data class Failed(val reason: LookupResult.Reason) : Fetched
    }

    companion object {
        private const val MAX_LOOK_ALIKES = 8
        private val allowed = Regex("^[a-z][a-z' -]{0,44}$")

        /** Lower-cases and trims a query; returns null for input that cannot be a word. */
        fun normalise(text: String): String? =
            text.trim().lowercase().replace(Regex("\\s+"), " ").takeIf { it.matches(allowed) }
    }
}
