package au.edu.jcu.wordkeep.domain.spelling

/** The outcome of one spelling attempt. */
sealed interface SpellingVerdict {
    /** Correct, possibly using an accepted alternative spelling (e.g. "analyze" for "analyse"). */
    data class Correct(val usedAlternate: Boolean) : SpellingVerdict

    /** Close: [distance] letter edits away. The learner may try once more. */
    data class NearMiss(val distance: Int, val diff: List<LetterMark>) : SpellingVerdict

    data class Incorrect(val diff: List<LetterMark>) : SpellingVerdict
}

/** One letter of the correct spelling, marked so the UI can underline what was wrong. */
data class LetterMark(val letter: Char, val status: Status) {
    enum class Status { MATCHED, WRONG, MISSING }
}

/**
 * Checks a typed answer against the target word.
 *
 * Both British and American spellings are accepted when the dictionary lists them, because
 * learners from different school systems should not lose marks for a spelling convention.
 */
object SpellingJudge {

    fun judge(attempt: String, answer: String, alternates: List<String> = emptyList()): SpellingVerdict {
        val typed = normalise(attempt)
        val target = normalise(answer)
        if (typed == target) return SpellingVerdict.Correct(usedAlternate = false)
        if (alternates.any { normalise(it) == typed }) return SpellingVerdict.Correct(usedAlternate = true)

        val distance = levenshtein(typed, target)
        val diff = diff(typed, target)
        val nearMissLimit = if (target.length <= 4) 1 else 2
        return if (typed.isNotEmpty() && distance <= nearMissLimit) {
            SpellingVerdict.NearMiss(distance, diff)
        } else {
            SpellingVerdict.Incorrect(diff)
        }
    }

    internal fun normalise(text: String): String = text.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Classic dynamic-programming edit distance (insert, delete, substitute all cost 1). */
    fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }

    /**
     * Marks each letter of the correct word as matched, wrong or missing, using the
     * edit-distance table to find the best alignment with what was typed.
     */
    fun diff(typed: String, target: String): List<LetterMark> {
        val rows = typed.length
        val cols = target.length
        val table = Array(rows + 1) { IntArray(cols + 1) }
        for (i in 0..rows) table[i][0] = i
        for (j in 0..cols) table[0][j] = j
        for (i in 1..rows) for (j in 1..cols) {
            val cost = if (typed[i - 1] == target[j - 1]) 0 else 1
            table[i][j] = minOf(table[i - 1][j] + 1, table[i][j - 1] + 1, table[i - 1][j - 1] + cost)
        }

        val marks = ArrayDeque<LetterMark>()
        var i = rows
        var j = cols
        while (j > 0) {
            val letter = target[j - 1]
            when {
                i > 0 && typed[i - 1] == letter && table[i][j] == table[i - 1][j - 1] -> {
                    marks.addFirst(LetterMark(letter, LetterMark.Status.MATCHED)); i--; j--
                }
                i > 0 && table[i][j] == table[i - 1][j - 1] + 1 -> {
                    marks.addFirst(LetterMark(letter, LetterMark.Status.WRONG)); i--; j--
                }
                table[i][j] == table[i][j - 1] + 1 -> {
                    marks.addFirst(LetterMark(letter, LetterMark.Status.MISSING)); j--
                }
                else -> i-- // an extra typed letter: nothing to mark on the target word
            }
        }
        return marks.toList()
    }
}
