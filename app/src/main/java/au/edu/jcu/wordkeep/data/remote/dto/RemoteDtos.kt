package au.edu.jcu.wordkeep.data.remote.dto

import kotlinx.serialization.Serializable

// Free Dictionary API (https://dictionaryapi.dev). Every field has a default because the
// Wiktionary-derived data is inconsistent: phonetics, audio and examples are often missing.

@Serializable
data class DictionaryEntryDto(
    val word: String,
    val phonetic: String? = null,
    val phonetics: List<PhoneticDto> = emptyList(),
    val meanings: List<MeaningDto> = emptyList(),
    val sourceUrls: List<String> = emptyList(),
)

@Serializable
data class PhoneticDto(
    val text: String? = null,
    val audio: String? = null,
)

@Serializable
data class MeaningDto(
    val partOfSpeech: String = "",
    val definitions: List<DefinitionDto> = emptyList(),
    val synonyms: List<String> = emptyList(),
)

@Serializable
data class DefinitionDto(
    val definition: String = "",
    val example: String? = null,
    val synonyms: List<String> = emptyList(),
)

// Datamuse API (https://www.datamuse.com/api/)

@Serializable
data class DatamuseWordDto(
    val word: String,
    val score: Long = 0,
)
