package au.edu.jcu.wordkeep.data.local

import au.edu.jcu.wordkeep.data.local.entity.CardEntity
import au.edu.jcu.wordkeep.data.local.entity.SenseEntity
import au.edu.jcu.wordkeep.data.local.entity.WordEntity
import au.edu.jcu.wordkeep.data.local.entity.WordWithDetails
import au.edu.jcu.wordkeep.domain.model.DictionaryEntry
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.ReviewState
import au.edu.jcu.wordkeep.domain.model.Sense
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import java.time.Instant

// Conversions between Room entities and domain models. The UI and domain layers never see entities.

fun WordWithDetails.toStudyWord(): StudyWord = StudyWord(
    word = word.toDomain(senses),
    review = card?.toReviewState() ?: ReviewState(),
)

fun WordEntity.toDomain(senses: List<SenseEntity>): Word = Word(
    id = id,
    text = text,
    phonetic = phonetic,
    audioUrls = audioUrls,
    alternateSpellings = alternateSpellings,
    senses = senses.sortedBy { it.position }.map { it.toDomain() },
    origin = origin,
    fetchState = fetchState,
    addedAt = addedAt,
    note = note,
)

fun SenseEntity.toDomain(): Sense = Sense(
    partOfSpeech = partOfSpeech,
    definition = definition,
    example = example,
    synonyms = synonyms,
)

fun CardEntity.toReviewState(): ReviewState = ReviewState(
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    dueDate = dueDay,
    lapses = lapses,
    introducedOn = introducedDay,
)

fun ReviewState.toCardEntity(wordId: Long, reviewedAt: Instant): CardEntity = CardEntity(
    wordId = wordId,
    repetitions = repetitions,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    dueDay = dueDate,
    lapses = lapses,
    introducedDay = introducedOn,
    lastReviewedAt = reviewedAt,
)

fun DictionaryEntry.toWordEntity(origin: WordOrigin, now: Instant): WordEntity = WordEntity(
    text = text,
    phonetic = phonetic,
    audioUrls = audioUrls,
    alternateSpellings = alternateSpellings,
    origin = origin,
    fetchState = FetchState.READY,
    addedAt = now,
    fetchedAt = now,
)

fun DictionaryEntry.toSenseEntities(wordId: Long = 0): List<SenseEntity> =
    senses.mapIndexed { index, sense ->
        SenseEntity(
            wordId = wordId,
            position = index,
            partOfSpeech = sense.partOfSpeech,
            definition = sense.definition,
            example = sense.example,
            synonyms = sense.synonyms,
        )
    }

fun Word.toDictionaryEntry(): DictionaryEntry = DictionaryEntry(
    text = text,
    phonetic = phonetic,
    audioUrls = audioUrls,
    alternateSpellings = alternateSpellings,
    senses = senses,
)
