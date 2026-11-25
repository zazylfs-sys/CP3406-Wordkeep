package au.edu.jcu.wordkeep.domain.model

import java.time.Instant
import java.util.Locale

/**
 * A word the learner has saved, together with the dictionary content cached on the device.
 *
 * Dictionary content ([senses], [phonetic], [audioUrls]) is kept separate from learning
 * progress ([ReviewState]) so that progress can be reset without losing the definitions.
 */
data class Word(
    val id: Long,
    val text: String,
    val phonetic: String?,
    val audioUrls: List<String>,
    val alternateSpellings: List<String>,
    val senses: List<Sense>,
    val origin: WordOrigin,
    val fetchState: FetchState,
    val addedAt: Instant,
    val note: String? = null,
) {
    /** The sense shown on flashcards and used for quiz prompts. */
    val primarySense: Sense? get() = senses.firstOrNull()

    val isReady: Boolean get() = fetchState == FetchState.READY && senses.isNotEmpty()

    /**
     * Picks the recording that matches the learner's accent, falling back to any recording.
     * Free Dictionary API names its files like `hypothesis-us.mp3`.
     */
    fun audioFor(accent: Accent): String? =
        audioUrls.firstOrNull { it.substringAfterLast('/').contains(accent.fileSuffix) }
            ?: audioUrls.firstOrNull()
}

data class Sense(
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val synonyms: List<String>,
)

/** Where a saved word came from. Shown as a filter in the library. */
enum class WordOrigin { STARTER_PACK, LEARNER }

/** Whether the dictionary content for a saved word has been downloaded yet. */
enum class FetchState { PENDING, READY, NOT_FOUND }

/** Pronunciation preference. Used to pick a recording, and the text-to-speech voice as a fallback. */
enum class Accent(val label: String, val fileSuffix: String, val locale: Locale) {
    UK("UK", "-uk", Locale.UK),
    US("US", "-us", Locale.US),
    AU("Australia", "-au", Locale("en", "AU")),
}

/** A dictionary entry fetched from the network but not necessarily saved yet. */
data class DictionaryEntry(
    val text: String,
    val phonetic: String?,
    val audioUrls: List<String>,
    val alternateSpellings: List<String>,
    val senses: List<Sense>,
)
