package au.edu.jcu.wordkeep.domain.quiz

import au.edu.jcu.wordkeep.domain.model.Sense
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.spelling.SpellingJudge
import kotlin.random.Random

/** One "which word matches this definition?" question. */
data class MeaningQuestion(
    val word: Word,
    val sense: Sense,
    /** The definition with the target word (and its inflections) masked out. */
    val prompt: String,
    val options: List<String>,
    val answerIndex: Int,
) {
    val answer: String get() = options[answerIndex]
}

/**
 * Builds multiple-choice questions.
 *
 * Distractors come from two places: other words in the learner's own deck (same part of speech
 * first), and look-alike words from the Datamuse API such as "analyst" for "analyse". Look-alikes
 * test whether the learner knows the meaning rather than recognising the shape of the word.
 */
class MeaningQuizBuilder(private val random: Random = Random.Default) {

    fun build(
        target: Word,
        deck: List<Word>,
        lookAlikes: List<String>,
        optionCount: Int = DEFAULT_OPTIONS,
    ): MeaningQuestion? {
        val sense = target.primarySense ?: return null
        val needed = optionCount - 1

        val external = DistractorFilter.filter(target.text, lookAlikes).shuffled(random).take(MAX_LOOK_ALIKES)
        val fromDeck = deck
            .filter { it.id != target.id && it.isReady }
            .shuffled(random)
            .sortedByDescending { it.primarySense?.partOfSpeech == sense.partOfSpeech }
            .map { it.text.lowercase() }
            .let { DistractorFilter.filter(target.text, it) }

        // Look-alikes first, then deck words of the same part of speech (sortedBy is stable).
        val distractors = (external + fromDeck).distinct().take(needed)
        if (distractors.size < MIN_OPTIONS - 1) return null

        val options = (distractors + target.text.lowercase()).shuffled(random)
        return MeaningQuestion(
            word = target,
            sense = sense,
            prompt = DefinitionMasker.mask(sense.definition, target.text),
            options = options,
            answerIndex = options.indexOf(target.text.lowercase()),
        )
    }

    companion object {
        const val DEFAULT_OPTIONS = 4
        const val MIN_OPTIONS = 3
        private const val MAX_LOOK_ALIKES = 2
    }
}

/** Hides the answer when a definition mentions the word itself, e.g. "To subject to analysis". */
object DefinitionMasker {

    const val MASK = "____"

    fun mask(definition: String, word: String): String {
        val stem = stemOf(word)
        if (stem.length < 3) return definition
        val pattern = Regex("\\b${Regex.escape(stem)}[a-z]*", RegexOption.IGNORE_CASE)
        return pattern.replace(definition, MASK)
    }

    /** A rough stem: drop a trailing "e"/"y" and keep at most the first 5 letters (analyse -> analy). */
    internal fun stemOf(word: String): String {
        val lower = word.lowercase().trim()
        val trimmed = lower.trimEnd('e', 'y')
        return trimmed.take(maxOf(4, minOf(5, trimmed.length)))
    }
}

/**
 * Rules that keep distractors fair: real single words, never the answer, a form of it
 * ("analysed", "reapproach") or a near-misspelling of it (one letter away).
 */
object DistractorFilter {

    private val singleWord = Regex("^[a-z]+(-[a-z]+)?$")

    fun filter(target: String, candidates: List<String>): List<String> {
        val answer = target.lowercase().trim()
        val stem = DefinitionMasker.stemOf(answer)
        return candidates
            .map { it.lowercase().trim() }
            .filter { it.matches(singleWord) }
            .filter { it != answer && !it.contains(stem) }
            .filter { SpellingJudge.levenshtein(it, answer) > 1 }
            .filter { it.length >= 3 }
            .distinct()
    }
}
