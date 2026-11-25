package au.edu.jcu.wordkeep.ui

import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.ReviewState
import au.edu.jcu.wordkeep.domain.model.Sense
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import java.time.Instant

fun studyWord(id: Long, text: String, definition: String = "A test definition of $text.") = StudyWord(
    word = Word(
        id = id,
        text = text,
        phonetic = "/test/",
        audioUrls = emptyList(),
        alternateSpellings = emptyList(),
        senses = listOf(Sense("noun", definition, "An example with $text.", emptyList())),
        origin = WordOrigin.LEARNER,
        fetchState = FetchState.READY,
        addedAt = Instant.EPOCH,
    ),
    review = ReviewState(),
)
