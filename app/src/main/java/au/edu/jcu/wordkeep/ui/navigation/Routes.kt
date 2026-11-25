package au.edu.jcu.wordkeep.ui.navigation

import au.edu.jcu.wordkeep.domain.model.StudyMode
import kotlinx.serialization.Serializable

// Type-safe Navigation routes. Arguments are checked at compile time instead of being
// assembled into route strings by hand.

@Serializable
data object OnboardingRoute

@Serializable
data object HomeRoute

/** Nested graph for the word library, its look-up screen and word details. */
@Serializable
data object WordsGraph

@Serializable
data class LibraryRoute(val query: String = "")

@Serializable
data class WordDetailRoute(val wordId: Long)

/** Also reachable from outside the app: `wordkeep://lookup/<word>`. */
@Serializable
data class LookupRoute(val word: String)

@Serializable
data object StatsRoute

/** Nested graph for settings and the "Your data" privacy screen. */
@Serializable
data object SettingsGraph

@Serializable
data object SettingsRoute

@Serializable
data object YourDataRoute

/** A study session. The bottom navigation is hidden so the learner can focus. */
@Serializable
data class SessionRoute(val mode: StudyMode)

@Serializable
data class SummaryRoute(
    val mode: StudyMode,
    val answered: Int,
    val correct: Int,
    val seconds: Int,
    val missedWordIds: LongArray,
)

const val LOOKUP_DEEP_LINK = "wordkeep://lookup"
