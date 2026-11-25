package au.edu.jcu.wordkeep

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import au.edu.jcu.wordkeep.data.local.WordkeepDatabase
import au.edu.jcu.wordkeep.data.local.dao.CardDao
import au.edu.jcu.wordkeep.data.local.dao.LookAlikeDao
import au.edu.jcu.wordkeep.data.local.dao.ReviewLogDao
import au.edu.jcu.wordkeep.data.local.dao.WordDao
import au.edu.jcu.wordkeep.data.remote.DatamuseApi
import au.edu.jcu.wordkeep.data.remote.DictionaryApi
import au.edu.jcu.wordkeep.data.remote.dto.DatamuseWordDto
import au.edu.jcu.wordkeep.data.remote.dto.DefinitionDto
import au.edu.jcu.wordkeep.data.remote.dto.DictionaryEntryDto
import au.edu.jcu.wordkeep.data.remote.dto.MeaningDto
import au.edu.jcu.wordkeep.data.remote.dto.PhoneticDto
import au.edu.jcu.wordkeep.di.AppModule
import au.edu.jcu.wordkeep.di.ApplicationScope
import au.edu.jcu.wordkeep.di.DatabaseModule
import au.edu.jcu.wordkeep.di.IoDispatcher
import au.edu.jcu.wordkeep.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.Clock
import java.util.UUID
import javax.inject.Singleton
import kotlin.random.Random

/** Canned dictionary: every word exists except "qwzxv". No real network traffic in tests. */
class FakeDictionaryApi : DictionaryApi {
    override suspend fun lookUp(word: String): List<DictionaryEntryDto> {
        if (word == "qwzxv") throw HttpException(Response.error<Any>(404, "{}".toResponseBody()))
        return listOf(
            DictionaryEntryDto(
                word = word,
                phonetics = listOf(PhoneticDto(text = "/$word/", audio = "")),
                meanings = listOf(
                    MeaningDto(
                        partOfSpeech = "noun",
                        definitions = listOf(DefinitionDto(definition = "Test meaning of $word.", example = "An example.")),
                    )
                ),
            )
        )
    }
}

class FakeDatamuseApi : DatamuseApi {
    override suspend fun spelledLike(pattern: String, max: Int) =
        listOf("alpha", "bravo", "charlie").map { DatamuseWordDto(it) }

    override suspend fun soundsLike(word: String, max: Int) = emptyList<DatamuseWordDto>()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object FakeNetworkModule {
    @Provides
    @Singleton
    fun dictionaryApi(): DictionaryApi = FakeDictionaryApi()

    @Provides
    @Singleton
    fun datamuseApi(): DatamuseApi = FakeDatamuseApi()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object InMemoryDatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): WordkeepDatabase =
        Room.inMemoryDatabaseBuilder(context, WordkeepDatabase::class.java).build()

    @Provides
    fun wordDao(db: WordkeepDatabase): WordDao = db.wordDao()

    @Provides
    fun cardDao(db: WordkeepDatabase): CardDao = db.cardDao()

    @Provides
    fun reviewLogDao(db: WordkeepDatabase): ReviewLogDao = db.reviewLogDao()

    @Provides
    fun lookAlikeDao(db: WordkeepDatabase): LookAlikeDao = db.lookAlikeDao()
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AppModule::class])
object TestAppModule {
    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun random(): Random = Random(7)

    @Provides
    @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** A fresh settings file per test, so every test starts at the first-run screen. */
    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("test-${UUID.randomUUID()}") }
}
