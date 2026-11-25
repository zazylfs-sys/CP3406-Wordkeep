package au.edu.jcu.wordkeep.domain

import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.Sense
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import java.time.Instant

internal fun testWord(id: Long, text: String, partOfSpeech: String = "noun") = Word(
    id = id,
    text = text,
    phonetic = null,
    audioUrls = emptyList(),
    alternateSpellings = emptyList(),
    senses = listOf(Sense(partOfSpeech, "A sample definition.", null, emptyList())),
    origin = WordOrigin.LEARNER,
    fetchState = FetchState.READY,
    addedAt = Instant.EPOCH,
)
