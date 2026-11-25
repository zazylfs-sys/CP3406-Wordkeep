package au.edu.jcu.wordkeep.di

import au.edu.jcu.wordkeep.data.remote.DatamuseApi
import au.edu.jcu.wordkeep.data.remote.DictionaryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /**
     * Requests carry a generic User-Agent and nothing that identifies the learner: no account,
     * device id or location. The only data sent is the word being looked up.
     */
    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", "Wordkeep-Android/1.0").build())
        }
        .build()

    @Provides
    @Singleton
    fun provideDictionaryApi(client: OkHttpClient, json: Json): DictionaryApi =
        retrofit(DictionaryApi.BASE_URL, client, json).create(DictionaryApi::class.java)

    @Provides
    @Singleton
    fun provideDatamuseApi(client: OkHttpClient, json: Json): DatamuseApi =
        retrofit(DatamuseApi.BASE_URL, client, json).create(DatamuseApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
