package au.edu.jcu.wordkeep.data.repository

import au.edu.jcu.wordkeep.data.local.dao.CardDao
import au.edu.jcu.wordkeep.data.local.dao.LookAlikeDao
import au.edu.jcu.wordkeep.data.local.dao.WordDao
import au.edu.jcu.wordkeep.data.local.entity.CardEntity
import au.edu.jcu.wordkeep.data.local.entity.LookAlikeEntity
import au.edu.jcu.wordkeep.data.local.entity.SenseEntity
import au.edu.jcu.wordkeep.data.local.entity.WordEntity
import au.edu.jcu.wordkeep.data.local.entity.WordWithDetails
import au.edu.jcu.wordkeep.data.local.toDomain
import au.edu.jcu.wordkeep.data.remote.DatamuseApi
import au.edu.jcu.wordkeep.data.remote.DictionaryApi
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.di.NetworkModule
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import au.edu.jcu.wordkeep.domain.testWord
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstWordRepositoryTest {
    @get:Rule
    val server = MockWebServer().apply {
        dispatcher = QueueDispatcher().apply { setFailFast(true) }
    }

    private val clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC)
    private val words = MutableStateFlow<Map<Long, WordEntity>>(emptyMap())
    private val cards = InMemoryCardDao(words)
    private val wordDao = InMemoryWordDao(words, cards)
    private val lookAlikeDao = InMemoryLookAlikeDao()
    private val settings = InMemorySettingsRepository()

    @Test
    fun `lookup maps a successful dictionary response without saving it`() = runTest {
        val repository = repository()
        server.enqueue(fixtureResponse("hypothesis"))

        val found = repository.lookUp("  Hypothesis  ") as LookupResult.Found

        assertEquals("hypothesis", found.entry.text)
        assertTrue(found.entry.senses.isNotEmpty())
        assertNull(found.savedWordId)
        assertNull(wordDao.findByText("hypothesis"))
        assertEquals("/api/v2/entries/en/hypothesis", nextRequestPath())
    }

    @Test
    fun `lookup maps a 404 to NotFound`() = runTest {
        val repository = repository()
        server.enqueue(fixtureResponse("notfound", status = 404))

        assertEquals(LookupResult.NotFound("zzword"), repository.lookUp(" ZZWord "))
        assertEquals("/api/v2/entries/en/zzword", nextRequestPath())
    }

    @Test
    fun `lookup maps a disconnected response to NO_CONNECTION`() = runTest {
        val repository = repository()
        server.enqueue(fixtureResponse("hypothesis").setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))

        assertEquals(LookupResult.Unavailable(LookupResult.Reason.NO_CONNECTION), repository.lookUp("hypothesis"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `offline mode returns OFFLINE_MODE without sending a request`() = runTest {
        val repository = repository()
        settings.update { it.copy(offlineMode = true) }

        assertEquals(LookupResult.Unavailable(LookupResult.Reason.OFFLINE_MODE), repository.lookUp("concept"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `numeric input is invalid without sending a request`() = runTest {
        assertEquals(LookupResult.Invalid, repository().lookUp("123"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a saved ready word is returned without another network request`() = runTest {
        val repository = repository()
        server.enqueue(fixtureResponse("concept"))
        val entry = (repository.lookUp("concept") as LookupResult.Found).entry
        val id = repository.save(entry)

        assertEquals(LookupResult.Found(entry, id), repository.lookUp("Concept"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `save persists the word its senses and a card once`() = runTest {
        val repository = repository()
        server.enqueue(fixtureResponse("hypothesis"))
        val entry = (repository.lookUp("hypothesis") as LookupResult.Found).entry

        val id = repository.save(entry)
        assertEquals(id, repository.save(entry))

        val stored = wordDao.getAll().single()
        assertEquals(id, stored.word.id)
        assertEquals(entry.text, stored.word.text)
        assertEquals(entry.phonetic, stored.word.phonetic)
        assertEquals(entry.audioUrls, stored.word.audioUrls)
        assertEquals(WordOrigin.LEARNER, stored.word.origin)
        assertEquals(FetchState.READY, stored.word.fetchState)
        assertEquals(clock.instant(), stored.word.addedAt)
        assertEquals(entry.senses, stored.senses.map { it.toDomain() })
        assertTrue(stored.senses.all { it.wordId == id })
        assertEquals(listOf(CardEntity.newFor(id)), cards.observeAll().first())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `starter pack inserts sixty pending words only once`() = runTest {
        val repository = repository()

        assertEquals(60, repository.addStarterPack())
        val firstInsert = wordDao.getAll()
        assertEquals(0, repository.addStarterPack())

        assertEquals(firstInsert, wordDao.getAll())
        assertEquals(60, firstInsert.map { it.word.text }.distinct().size)
        assertEquals(60, repository.observePendingCount().first())
        assertTrue(firstInsert.all { it.word.origin == WordOrigin.STARTER_PACK && it.senses.isEmpty() })
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `pending download stores ready content and marks a 404 as NOT_FOUND`() = runTest {
        val repository = repository()
        val foundId = addPending("analyse")
        val missingId = addPending("zzword")
        respondByPath("analyse" to fixtureResponse("analyse"), "zzword" to fixtureResponse("notfound", status = 404))
        val progress = mutableListOf<Pair<Int, Int>>()

        assertEquals(DownloadSummary(1, 1, 0), repository.downloadPending { done, total -> progress += done to total })

        val found = checkNotNull(repository.observeWord(foundId).first()).word
        assertTrue(found.isReady)
        assertEquals(listOf("analyze"), found.alternateSpellings)
        assertEquals(FetchState.NOT_FOUND, wordDao.get(missingId)?.fetchState)
        assertEquals(clock.instant(), wordDao.get(foundId)?.fetchedAt)
        assertEquals(clock.instant(), wordDao.get(missingId)?.fetchedAt)
        // Words are fetched in parallel batches; progress is reported before and after each batch.
        assertEquals(listOf(0 to 2, 2 to 2), progress)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `pending download stops at a disconnect and leaves the failed word pending`() = runTest {
        val repository = repository()
        val readyId = addPending("concept")
        val interruptedId = addPending("hypothesis")
        respondByPath(
            "concept" to fixtureResponse("concept"),
            "hypothesis" to fixtureResponse("hypothesis").setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )

        val summary = repository.downloadPending()

        assertEquals(DownloadSummary(1, 0, 1, LookupResult.Reason.NO_CONNECTION), summary)
        assertEquals(FetchState.READY, wordDao.get(readyId)?.fetchState)
        assertEquals(listOf(interruptedId), wordDao.getPending().map { it.id })
    }

    @Test
    fun `a rate-limited look-up waits for Retry-After and then succeeds`() = runTest {
        val repository = repository()
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2"))
        server.enqueue(fixtureResponse("concept"))

        val result = repository.lookUp("concept")

        assertTrue(result is LookupResult.Found)
        assertEquals(2, server.requestCount)
        // The retry happened after the server's requested wait, in virtual test time.
        assertTrue(testScheduler.currentTime >= 2_000)
    }

    @Test
    fun `a download never overwrites the learner's note`() = runTest {
        val repository = repository()
        val id = addPending("concept")
        repository.updateNote(id, "con + cept: a thought taken in")
        respondByPath("concept" to fixtureResponse("concept"))

        repository.downloadPending()

        assertEquals("con + cept: a thought taken in", wordDao.get(id)?.note)
        assertEquals(FetchState.READY, wordDao.get(id)?.fetchState)
    }

    @Test
    fun `a word deleted while its definition downloads is not recreated`() = runTest {
        val repository = repository()
        val id = addPending("concept")
        respondByPath("concept" to fixtureResponse("concept"))
        repository.delete(id)

        // Simulates the late response arriving after the delete: content for a missing id is dropped.
        wordDao.replaceContent(id, null, emptyList(), emptyList(), clock.instant(), emptyList())

        assertNull(wordDao.get(id))
        assertTrue(repository.observeLibrary().first().isEmpty())
    }

    @Test
    fun `look-alikes exclude inflections and use the cache on the second call`() = runTest {
        val repository = repository()
        val word = testWord(1, "analyse")
        server.enqueue(fixtureResponse("datamuse_sample"))
        server.enqueue(
            MockResponse().setBody(
                """[{"word":"analyses","tags":["f:3.1"]},{"word":"anaphase","tags":["f:0.2"]},{"word":"annals","tags":["f:2.5"]}]"""
            )
        )

        val first = repository.lookAlikes(word)

        // Inflections ("analyze", "psychoanalyse"), multi-word entries and rare words (< 1 per million) are dropped.
        assertEquals(listOf("study", "examine", "annals"), first)
        assertEquals(first, lookAlikeDao.forWord(word.id))
        assertEquals("/words?sp=analyse&max=20&md=f", nextRequestPath())
        assertEquals("/words?sl=analyse&max=20&md=f", nextRequestPath())
        assertEquals(first, repository.lookAlikes(word))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `look-alikes return empty on server error`() = runTest {
        val repository = repository()
        server.enqueue(MockResponse().setResponseCode(503))

        assertTrue(repository.lookAlikes(testWord(1, "analyse")).isEmpty())
        assertEquals("/words?sp=analyse&max=20&md=f", nextRequestPath())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `normalise trims lowercases and collapses whitespace in valid words`() {
        val valid = mapOf(
            " \tMOTHER-IN-LAW\n" to "mother-in-law",
            "DON'T" to "don't",
            "  in\t SPITE\n of  " to "in spite of",
            "A" to "a",
            "A".repeat(45) to "a".repeat(45),
        )
        valid.forEach { (input, expected) ->
            assertEquals(input, expected, OfflineFirstWordRepository.normalise(input))
        }
    }

    @Test
    fun `normalise rejects invalid characters and queries longer than forty-five characters`() {
        listOf("", "  ", "123", "word2", "-word", "'word", "word!", "café", "a".repeat(46)).forEach { input ->
            assertNull(input, OfflineFirstWordRepository.normalise(input))
        }
    }

    private fun TestScope.repository(): OfflineFirstWordRepository {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(NetworkModule.provideOkHttp())
            .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
            .build()
        return OfflineFirstWordRepository(
            wordDao, cards, lookAlikeDao,
            retrofit.create(DictionaryApi::class.java), retrofit.create(DatamuseApi::class.java),
            settings, clock, StandardTestDispatcher(testScheduler),
        )
    }

    private suspend fun addPending(text: String): Long = wordDao.insertWithContent(
        WordEntity(
            text = text,
            phonetic = null,
            audioUrls = emptyList(),
            alternateSpellings = emptyList(),
            origin = WordOrigin.STARTER_PACK,
            fetchState = FetchState.PENDING,
            addedAt = clock.instant().plusMillis(words.value.size.toLong()),
            fetchedAt = null,
        ),
        emptyList(),
    )

    private fun fixtureResponse(name: String, status: Int = 200) = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(checkNotNull(javaClass.getResource("/dictionary/$name.json")).readText())

    private fun nextRequestPath(): String? = server.takeRequest(1, TimeUnit.SECONDS)?.path

    /** Answers by word, because parallel downloads do not arrive in a fixed order. */
    private fun respondByPath(vararg responses: Pair<String, MockResponse>) {
        val byWord = responses.toMap()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                byWord[request.path.orEmpty().substringAfterLast('/')] ?: MockResponse().setResponseCode(500)
        }
    }
}

internal class InMemorySettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (UserSettings) -> UserSettings) = settings.update(transform)

    override suspend fun clear() {
        settings.value = UserSettings()
    }
}

private class InMemoryWordDao(
    private val words: MutableStateFlow<Map<Long, WordEntity>>,
    private val cards: InMemoryCardDao,
) : WordDao {
    private val senses = MutableStateFlow<List<SenseEntity>>(emptyList())
    private var nextId = 1L

    private val details = combine(words, senses, cards.observeAll()) { rows, definitions, schedules ->
        rows.values.map { word ->
            WordWithDetails(word, definitions.filter { it.wordId == word.id }, schedules.firstOrNull { it.wordId == word.id })
        }
    }

    override fun observeAll() = details.map { it.sortedBy { row -> row.word.text.lowercase() } }

    override fun observe(id: Long) = details.map { it.firstOrNull { row -> row.word.id == id } }

    override suspend fun getAll() = details.first().sortedBy { it.word.addedAt }

    override suspend fun get(id: Long) = words.value[id]

    override suspend fun findByText(text: String) = words.value.values.firstOrNull { it.text.equals(text, ignoreCase = true) }

    override suspend fun getPending() = words.value.values.filter { it.fetchState == FetchState.PENDING }.sortedBy { it.addedAt }

    override fun observePendingCount() = words.map { rows -> rows.values.count { it.fetchState == FetchState.PENDING } }

    override suspend fun insert(word: WordEntity): Long {
        val id = if (word.id == 0L) nextId else word.id
        if (findByText(word.text) != null || id in words.value) return -1L
        nextId = maxOf(nextId, id + 1)
        words.update { it + (id to word.copy(id = id)) }
        return id
    }

    override suspend fun update(word: WordEntity) {
        if (word.id in words.value) words.update { it + (word.id to word) }
    }

    override suspend fun updateDictionaryContent(
        id: Long,
        phonetic: String?,
        audioUrls: List<String>,
        alternateSpellings: List<String>,
        fetchedAt: Instant,
    ): Int {
        val word = words.value[id] ?: return 0
        update(
            word.copy(
                phonetic = phonetic,
                audioUrls = audioUrls,
                alternateSpellings = alternateSpellings,
                fetchState = FetchState.READY,
                fetchedAt = fetchedAt,
            )
        )
        return 1
    }

    override suspend fun updateFetchState(id: Long, state: FetchState, fetchedAt: Instant): Int {
        val word = words.value[id] ?: return 0
        update(word.copy(fetchState = state, fetchedAt = fetchedAt))
        return 1
    }

    override suspend fun insertSenses(senses: List<SenseEntity>) = this.senses.update { it + senses }

    override suspend fun deleteSenses(wordId: Long) = senses.update { rows -> rows.filterNot { it.wordId == wordId } }

    override suspend fun insertCard(card: CardEntity) {
        if (cards.get(card.wordId) == null) cards.upsert(card)
    }

    override suspend fun updateNote(id: Long, note: String?) {
        get(id)?.let { update(it.copy(note = note)) }
    }

    override suspend fun delete(id: Long) {
        words.update { it - id }
        deleteSenses(id)
        cards.delete(id)
    }
}

private class InMemoryCardDao(private val words: MutableStateFlow<Map<Long, WordEntity>>) : CardDao {
    private val cards = MutableStateFlow<Map<Long, CardEntity>>(emptyMap())

    override suspend fun upsert(card: CardEntity) = cards.update { it + (card.wordId to card) }

    override suspend fun get(wordId: Long) = cards.value[wordId]

    override fun observeAll() = cards.map { it.values.toList() }

    override fun observeDueCount(today: LocalDate) = combine(cards, words) { schedules, rows ->
        schedules.values.count { card ->
            val due = card.dueDay
            due != null && due <= today && rows[card.wordId]?.fetchState == FetchState.READY
        }
    }

    override fun observeIntroducedOn(day: LocalDate) = cards.map { rows -> rows.values.count { it.introducedDay == day } }

    override suspend fun reset(wordId: Long) {
        if (wordId in cards.value) upsert(CardEntity.newFor(wordId))
    }

    fun delete(wordId: Long) = cards.update { it - wordId }
}

private class InMemoryLookAlikeDao : LookAlikeDao {
    private val rows = linkedSetOf<LookAlikeEntity>()

    override suspend fun forWord(wordId: Long) = rows.filter { it.wordId == wordId }.map { it.text }

    override suspend fun insertAll(items: List<LookAlikeEntity>) {
        rows.addAll(items)
    }
}
