package au.edu.jcu.wordkeep.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import au.edu.jcu.wordkeep.data.repository.OfflineFirstWordRepository
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.domain.StarterPack
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.MasteryLevel
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import au.edu.jcu.wordkeep.ui.navigation.LibraryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter(val label: String) {
    ALL("All"),
    NEW("New"),
    LEARNING("Learning"),
    MASTERED("Mastered"),
    STARTER("Starter list"),
}

data class LibraryUiState(
    val loading: Boolean = true,
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.ALL,
    val words: List<StudyWord> = emptyList(),
    val totalWords: Int = 0,
    /** Non-null when the query could be looked up and is not already saved. */
    val lookUpSuggestion: String? = null,
    val starterPackAdded: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
) : ViewModel() {

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, savedStateHandle.toRoute<LibraryRoute>().query)
    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, LibraryFilter.ALL)

    val uiState: StateFlow<LibraryUiState> = combine(wordRepository.observeLibrary(), query, filter) { words, q, f ->
        val normalised = q.trim().lowercase()
        val visible = words
            .filter { normalised.isEmpty() || it.word.text.contains(normalised) }
            .filter { matches(it, f) }
        val exactMatch = words.any { it.word.text == normalised }
        LibraryUiState(
            loading = false,
            query = q,
            filter = f,
            words = visible,
            totalWords = words.size,
            lookUpSuggestion = OfflineFirstWordRepository.normalise(q)?.takeIf { !exactMatch },
            starterPackAdded = words.any { it.word.origin == WordOrigin.STARTER_PACK },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun onFilterChange(value: LibraryFilter) {
        savedStateHandle[KEY_FILTER] = value
    }

    fun addStarterPack() {
        viewModelScope.launch {
            if (wordRepository.addStarterPack() > 0) wordRepository.downloadPending()
        }
    }

    private fun matches(word: StudyWord, filter: LibraryFilter): Boolean = when (filter) {
        LibraryFilter.ALL -> true
        LibraryFilter.NEW -> word.review.mastery == MasteryLevel.NEW
        LibraryFilter.LEARNING -> word.review.mastery == MasteryLevel.LEARNING
        LibraryFilter.MASTERED -> word.review.mastery == MasteryLevel.MASTERED
        LibraryFilter.STARTER -> word.word.origin == WordOrigin.STARTER_PACK
    }

    companion object {
        private const val KEY_QUERY = "library_query"
        private const val KEY_FILTER = "library_filter"
        val starterPackSize = StarterPack.words.size

        fun statusLine(word: StudyWord): String = when (word.word.fetchState) {
            FetchState.PENDING -> "Definition not downloaded yet"
            FetchState.NOT_FOUND -> "Not found in the dictionary"
            FetchState.READY -> word.word.primarySense?.definition.orEmpty()
        }
    }
}
