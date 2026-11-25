package au.edu.jcu.wordkeep.domain.quiz

import au.edu.jcu.wordkeep.domain.testWord
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeaningQuizBuilderTest {
    private val builder = MeaningQuizBuilder(Random(42))

    @Test
    fun `the answer appears once and the index points to it among distinct options`() {
        val target = testWord(1, "Analyse", partOfSpeech = "verb")
        val deck = listOf(target, testWord(2, "compare"), testWord(3, "review"), testWord(4, "inspect"))

        val question = requireNotNull(builder.build(target, deck, listOf("ANALYSE", "compare", " Compare ")))

        assertEquals(4, question.options.size)
        assertEquals(question.options.size, question.options.toSet().size)
        assertEquals(1, question.options.count { it == "analyse" })
        assertEquals("analyse", question.options[question.answerIndex])
    }

    @Test
    fun `answer inflections and multi-word candidates are excluded from both sources`() {
        val target = testWord(1, "analyse", partOfSpeech = "verb")
        val deck = listOf(
            testWord(2, "analysed", partOfSpeech = "verb"),
            testWord(3, "look into", partOfSpeech = "verb"),
            testWord(4, "inspect", partOfSpeech = "verb"),
            testWord(5, "review", partOfSpeech = "verb"),
        )
        val lookAlikes = listOf("analysis", "check out", "compare")

        val question = requireNotNull(builder.build(target, deck, lookAlikes, optionCount = 7))

        assertEquals(setOf("analyse", "inspect", "review", "compare"), question.options.toSet())
    }

    @Test
    fun `too few distinct eligible options returns null`() {
        val target = testWord(1, "analyse", partOfSpeech = "verb")
        val deck = listOf(testWord(2, "inspect"))

        assertNull(builder.build(target, deck, listOf("INSPECT", "analysis", "look into")))
    }

    @Test
    fun `the minimum number of eligible options is enough to build a question`() {
        val target = testWord(1, "analyse", partOfSpeech = "verb")

        val question = requireNotNull(builder.build(target, emptyList(), listOf("inspect", "compare")))

        assertEquals(setOf("analyse", "inspect", "compare"), question.options.toSet())
    }

    @Test
    fun `a target without a sense returns null even with enough distractors`() {
        val target = testWord(1, "analyse").copy(senses = emptyList())
        val deck = listOf(testWord(2, "inspect"), testWord(3, "compare"), testWord(4, "review"))

        assertNull(builder.build(target, deck, emptyList()))
    }

    @Test
    fun `deck distractors with the same part of speech fill the available slots first`() {
        val target = testWord(1, "analyse", partOfSpeech = "verb")
        val deck = listOf(
            testWord(2, "method"),
            testWord(3, "inspect", partOfSpeech = "verb"),
            testWord(4, "result"),
            testWord(5, "review", partOfSpeech = "verb"),
        )

        val question = requireNotNull(builder.build(target, deck, emptyList(), optionCount = 3))

        assertEquals(setOf("analyse", "inspect", "review"), question.options.toSet())
    }

    @Test
    fun `definition masker hides analysis for analyse and leaves unrelated words intact`() {
        assertEquals("To subject to ____.", DefinitionMasker.mask("To subject to analysis.", "analyse"))
        assertEquals(
            "To examine the evidence carefully.",
            DefinitionMasker.mask("To examine the evidence carefully.", "analyse"),
        )
    }
}
