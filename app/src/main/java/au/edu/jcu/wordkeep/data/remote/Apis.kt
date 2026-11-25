package au.edu.jcu.wordkeep.data.remote

import au.edu.jcu.wordkeep.data.remote.dto.DatamuseWordDto
import au.edu.jcu.wordkeep.data.remote.dto.DictionaryEntryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Free Dictionary API. Returns HTTP 404 when a word is not in the dictionary. */
interface DictionaryApi {
    @GET("api/v2/entries/en/{word}")
    suspend fun lookUp(@Path("word") word: String): List<DictionaryEntryDto>

    companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/"
    }
}

/** Datamuse word-finding API. Only the word being studied is sent. */
interface DatamuseApi {
    /** Words spelled like [pattern], e.g. `sp=analyse` gives analyst, analyses, ... */
    @GET("words")
    suspend fun spelledLike(
        @Query("sp") pattern: String,
        @Query("max") max: Int = 20,
    ): List<DatamuseWordDto>

    /** Words that sound like [word]. */
    @GET("words")
    suspend fun soundsLike(
        @Query("sl") word: String,
        @Query("max") max: Int = 20,
    ): List<DatamuseWordDto>

    companion object {
        const val BASE_URL = "https://api.datamuse.com/"
    }
}
