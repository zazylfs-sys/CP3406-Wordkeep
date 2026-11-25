package au.edu.jcu.wordkeep.data.remote

import au.edu.jcu.wordkeep.data.remote.dto.DictionaryEntryDto
import au.edu.jcu.wordkeep.domain.model.DictionaryEntry
import au.edu.jcu.wordkeep.domain.model.Sense
import java.net.URLDecoder

/**
 * Turns Free Dictionary API responses into a [DictionaryEntry].
 *
 * The API can return several entries for one word (one per etymology). They are merged, the
 * number of senses is capped so flashcards stay readable, and obviously broken phonetic strings
 * such as "/-əsəs/" (a suffix fragment from Wiktionary) are skipped.
 */
object DictionaryMapper {

    const val MAX_SENSES = 4
    private const val MAX_SYNONYMS = 6

    fun toEntry(requested: String, dtos: List<DictionaryEntryDto>): DictionaryEntry? {
        if (dtos.isEmpty()) return null
        val text = dtos.first().word.ifBlank { requested }.trim().lowercase()

        val senses = dtos
            .flatMap { entry -> entry.meanings }
            .flatMap { meaning ->
                meaning.definitions
                    .filter { it.definition.isNotBlank() }
                    .map { definition ->
                        Sense(
                            partOfSpeech = meaning.partOfSpeech.trim(),
                            definition = definition.definition.trim(),
                            example = definition.example?.trim()?.takeIf { it.isNotEmpty() },
                            synonyms = (definition.synonyms + meaning.synonyms).distinct().take(MAX_SYNONYMS),
                        )
                    }
            }
            .let(::spreadAcrossPartsOfSpeech)
            .take(MAX_SENSES)
        if (senses.isEmpty()) return null

        val phonetics = dtos.flatMap { it.phonetics }
        val audio = phonetics.mapNotNull { it.audio?.trim()?.takeIf(String::isNotEmpty) }.distinct()
        val phonetic = (phonetics.mapNotNull { it.text } + dtos.mapNotNull { it.phonetic })
            .map(String::trim)
            .firstOrNull(::isUsablePhonetic)

        return DictionaryEntry(
            text = text,
            phonetic = phonetic,
            audioUrls = audio,
            alternateSpellings = alternateSpellings(text, dtos),
            senses = senses,
        )
    }

    /**
     * Keeps the first sense of every part of speech before second senses, so a word that is
     * both a noun and a verb shows one of each instead of four noun senses.
     */
    internal fun spreadAcrossPartsOfSpeech(senses: List<Sense>): List<Sense> {
        val groups = senses.groupBy { it.partOfSpeech }.values.map { it.toMutableList() }
        val result = mutableListOf<Sense>()
        while (groups.any { it.isNotEmpty() }) {
            groups.forEach { group -> if (group.isNotEmpty()) result += group.removeAt(0) }
        }
        return result
    }

    internal fun isUsablePhonetic(text: String): Boolean =
        text.length > 2 && !text.startsWith("/-") && !text.startsWith("-") && text.any(Char::isLetter)

    /**
     * Wiktionary source URLs list spelling variants, e.g. "analyse" also links to ".../analyze".
     * Those are accepted as correct in the spelling activity.
     */
    internal fun alternateSpellings(text: String, dtos: List<DictionaryEntryDto>): List<String> =
        dtos.flatMap { it.sourceUrls }
            .map { URLDecoder.decode(it.substringAfterLast('/'), "UTF-8").lowercase() }
            .filter { it != text && it.matches(Regex("^[a-z-]+$")) }
            .distinct()
}
