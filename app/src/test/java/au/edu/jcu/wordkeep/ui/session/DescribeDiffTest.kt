package au.edu.jcu.wordkeep.ui.session

import au.edu.jcu.wordkeep.domain.spelling.SpellingJudge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The spoken/text version of the underlined spelling mistakes. */
class DescribeDiffTest {

    @Test
    fun `names each missed or changed letter with its position`() {
        val diff = SpellingJudge.diff("enviroment", "environment")
        assertEquals("Check letter 7 (n).", describeDiff(diff)) // e-n-v-i-r-o-[n]-m-e-n-t
    }

    @Test
    fun `joins several positions into one sentence`() {
        val diff = SpellingJudge.diff("cut", "cat")
        assertEquals("Check letter 2 (a).", describeDiff(diff))
        assertEquals("Check letter 1 (d), letter 2 (a) and letter 3 (t).", describeDiff(SpellingJudge.diff("xyz", "dat")))
    }

    @Test
    fun `returns null when nothing is wrong`() {
        assertNull(describeDiff(SpellingJudge.diff("data", "data")))
    }
}
