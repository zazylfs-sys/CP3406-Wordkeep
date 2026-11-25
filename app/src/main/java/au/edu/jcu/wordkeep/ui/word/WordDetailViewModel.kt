package au.edu.jcu.wordkeep.ui.word

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.audio.Pronouncer
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.ui.navigation.WordDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface WordDetailUiState {
    data object Loading : WordDetailUiState
    data object Missing : WordDetailUiState
    data class Ready(val word: StudyWord, val today: LocalDate) : WordDetailUiState
}

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val pronouncer: Pronouncer,
    clock: Clock,
) : ViewModel() {

    private val wordId = savedStateHandle.toRoute<WordDetailRoute>().wordId
    private val today = LocalDate.now(clock)

    val uiState: StateFlow<WordDetailUiState> = wordRepository.observeWord(wordId)
        .map { word -> word?.let { WordDetailUiState.Ready(it, today) } ?: WordDetailUiState.Missing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordDetailUiState.Loading)

    val playback: StateFlow<PlaybackState> = pronouncer.state

    /** One-shot signal to leave the screen after the word is deleted. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun pronounce() {
        val word = (uiState.value as? WordDetailUiState.Ready)?.word?.word ?: return
        viewModelScope.launch {
            val prefs = settingsRepository.settings.first()
            pronouncer.pronounce(word, prefs.accent, allowNetwork = !prefs.offlineMode)
        }
    }

    fun saveNote(note: String) {
        viewModelScope.launch { wordRepository.updateNote(wordId, note) }
    }

    fun resetProgress() {
        viewModelScope.launch { wordRepository.resetProgress(wordId) }
    }

    fun retryDownload() {
        viewModelScope.launch { wordRepository.downloadPending() }
    }

    fun delete() {
        viewModelScope.launch {
            pronouncer.stop()
            wordRepository.delete(wordId)
            _deleted.value = true
        }
    }

    override fun onCleared() {
        pronouncer.stop()
    }
}
