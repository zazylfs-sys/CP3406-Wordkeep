package au.edu.jcu.wordkeep.data

import au.edu.jcu.wordkeep.data.local.entity.SenseEntity
import au.edu.jcu.wordkeep.data.local.entity.WordEntity
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import java.time.Instant

fun wordEntity(text: String, state: FetchState = FetchState.READY, addedAt: Long = 0) = WordEntity(
    text = text,
    phonetic = "/$text/",
    audioUrls = listOf("https://example.org/$text-uk.mp3"),
    alternateSpellings = emptyList(),
    origin = WordOrigin.LEARNER,
    fetchState = state,
    addedAt = Instant.ofEpochMilli(addedAt),
    fetchedAt = null,
)

fun senses(vararg definitions: String) = definitions.mapIndexed { i, d ->
    SenseEntity(wordId = 0, position = i, partOfSpeech = "noun", definition = d, example = null, synonyms = listOf("a, b"))
}
