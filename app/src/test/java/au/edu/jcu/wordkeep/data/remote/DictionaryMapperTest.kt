package au.edu.jcu.wordkeep.data.remote

import au.edu.jcu.wordkeep.data.remote.dto.DefinitionDto
import au.edu.jcu.wordkeep.data.remote.dto.DictionaryEntryDto
import au.edu.jcu.wordkeep.data.remote.dto.MeaningDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryMapperTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `hypothesis skips the broken phonetic and keeps the US recording`() {
        val entry = checkNotNull(DictionaryMapper.toEntry("hypothesis", fixture("hypothesis")))

        assertEquals("/haɪˈpɑː.θə.sɪs/", entry.phonetic)
        assertEquals(
            listOf("https://api.dictionaryapi.dev/media/pronunciations/en/hypothesis-us.mp3"),
            entry.audioUrls,
        )
    }

    @Test
    fun `concept drops empty audio strings`() {
        val entry = checkNotNull(DictionaryMapper.toEntry("concept", fixture("concept")))

        assertTrue(entry.audioUrls.isEmpty())
    }

    @Test
    fun `analyse picks up its alternate spelling without phonetics`() {
        val entry = checkNotNull(DictionaryMapper.toEntry("analyse", fixture("analyse")))

        assertEquals(listOf("analyze"), entry.alternateSpellings)
        assertNull(entry.phonetic)
    }

    @Test
    fun `senses are capped after taking turns across parts of speech`() {
        val concept = fixture("concept").single()
        val verb = MeaningDto(
            partOfSpeech = "verb",
            definitions = listOf(DefinitionDto("To form a concept."), DefinitionDto("To develop a design.")),
        )
        val mixedConcept = concept.copy(meanings = concept.meanings + verb)

        val entry = checkNotNull(DictionaryMapper.toEntry("concept", listOf(mixedConcept)))

        assertEquals(DictionaryMapper.MAX_SENSES, entry.senses.size)
        assertEquals(listOf("noun", "verb", "noun", "verb"), entry.senses.map { it.partOfSpeech })
    }

    @Test
    fun `empty response has no entry`() {
        assertNull(DictionaryMapper.toEntry("concept", emptyList()))
    }

    private fun fixture(name: String): List<DictionaryEntryDto> =
        json.decodeFromString(checkNotNull(javaClass.getResource("/dictionary/$name.json")).readText())
}
