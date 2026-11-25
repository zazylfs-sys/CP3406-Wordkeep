package au.edu.jcu.wordkeep.domain.spelling

import au.edu.jcu.wordkeep.domain.spelling.LetterMark.Status
import au.edu.jcu.wordkeep.domain.spelling.LetterMark.Status.MATCHED
import au.edu.jcu.wordkeep.domain.spelling.LetterMark.Status.MISSING
import au.edu.jcu.wordkeep.domain.spelling.LetterMark.Status.WRONG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellingJudgeTest {
    @Test
    fun `exact spelling ignores case and surrounding whitespace`() {
        assertEquals(SpellingVerdict.Correct(usedAlternate = false), SpellingJudge.judge(" \tAnAlYsE\n", "analyse"))
    }

    @Test
    fun `an accepted alternate spelling is marked correct and flagged as alternate`() {
        val verdict = SpellingJudge.judge(" ANALYZE ", "analyse", alternates = listOf("analyze"))

        assertEquals(SpellingVerdict.Correct(usedAlternate = true), verdict)
    }

    @Test
    fun `a four-letter target allows one edit and rejects two`() {
        val near = SpellingJudge.judge("bark", "dark")

        assertTrue(near is SpellingVerdict.NearMiss)
        assertEquals(1, (near as SpellingVerdict.NearMiss).distance)
        assertTrue(SpellingJudge.judge("dusk", "dark") is SpellingVerdict.Incorrect)
    }

    @Test
    fun `a five-letter target allows two edits and rejects three`() {
        val near = SpellingJudge.judge("bland", "plant")

        assertTrue(near is SpellingVerdict.NearMiss)
        assertEquals(2, (near as SpellingVerdict.NearMiss).distance)
        assertTrue(SpellingJudge.judge("brand", "plant") is SpellingVerdict.Incorrect)
    }

    @Test
    fun `empty and whitespace-only attempts are incorrect within the near miss limit`() {
        val expected = SpellingVerdict.Incorrect(listOf(LetterMark('a', MISSING)))

        assertEquals(expected, SpellingJudge.judge("", "a"))
        assertEquals(expected, SpellingJudge.judge(" \t ", "a"))
    }

    @Test
    fun `Levenshtein distance covers three edits and empty inputs`() {
        assertEquals(3, SpellingJudge.levenshtein("kitten", "sitting"))
        assertEquals(0, SpellingJudge.levenshtein("", ""))
        assertEquals(3, SpellingJudge.levenshtein("", "cat"))
        assertEquals(3, SpellingJudge.levenshtein("cat", ""))
    }

    @Test
    fun `diff marks the missing n in enviroment and matches the remaining letters`() {
        val target = "environment"
        val statuses = List(target.length) { if (it == 6) MISSING else MATCHED }

        assertDiff("enviroment", target, statuses)
    }

    @Test
    fun `diff marks the target letter wrong at a substitution`() {
        assertDiff("cot", "cat", listOf(MATCHED, WRONG, MATCHED))
    }

    @Test
    fun `diff skips an extra typed letter and matches every target letter`() {
        val target = "plant"

        assertDiff("plxant", target, List(target.length) { MATCHED })
    }

    private fun assertDiff(typed: String, target: String, statuses: List<Status>) {
        val diff = SpellingJudge.diff(typed, target)

        assertEquals(target.length, diff.size)
        assertEquals(target, diff.joinToString("") { it.letter.toString() })
        assertEquals(statuses, diff.map { it.status })
    }
}
