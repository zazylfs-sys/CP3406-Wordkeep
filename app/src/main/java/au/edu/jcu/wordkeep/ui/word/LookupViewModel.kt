package au.edu.jcu.wordkeep.ui.word

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.data.audio.Pronouncer
import au.edu.jcu.wordkeep.data.repository.LookupResult
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.Word
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import au.edu.jcu.wordkeep.ui.navigation.LookupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

sealed interface LookupUiState {
    data object Loading : LookupUiState
    data class Result(val result: LookupResult, val saving: Boolean = false, val savedId: Long? = null) : LookupUiState
}

/** Shows a dictionary entry before anything is saved, so the learner decides what to keep. */
@HiltViewModel
class LookupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val pronouncer: Pronouncer,
) : ViewModel() {

    val query: String = savedStateHandle.toRoute<LookupRoute>().word

    private val _uiState = MutableStateFlow<LookupUiState>(LookupUiState.Loading)
    val uiState: StateFlow<LookupUiState> = _uiState.asStateFlow()
    val playback: StateFlow<PlaybackState> = pronouncer.state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = LookupUiState.Loading
            val result = wordRepository.lookUp(query)
            _uiState.value = LookupUiState.Result(result, savedId = (result as? LookupResult.Found)?.savedWordId)
        }
    }

    fun save() {
        val current = _uiState.value as? LookupUiState.Result ?: return
        val found = current.result as? LookupResult.Found ?: return
        if (current.saving || current.savedId != null) return
        viewModelScope.launch {
            _uiState.value = current.copy(saving = true)
            val id = wordRepository.save(found.entry)
            _uiState.value = current.copy(saving = false, savedId = id)
        }
    }

    fun pronounce() {
        val found = (_uiState.value as? LookupUiState.Result)?.result as? LookupResult.Found ?: return
        viewModelScope.launch {
            val prefs = settingsRepository.settings.first()
            val entry = found.entry
            // A temporary Word so the pronouncer can pick the right accent recording.
            val word = Word(
                id = 0,
                text = entry.text,
                phonetic = entry.phonetic,
                audioUrls = entry.audioUrls,
                alternateSpellings = entry.alternateSpellings,
                senses = entry.senses,
                origin = WordOrigin.LEARNER,
                fetchState = FetchState.READY,
                addedAt = Instant.EPOCH,
            )
            pronouncer.pronounce(word, prefs.accent, allowNetwork = !prefs.offlineMode)
        }
    }

    override fun onCleared() {
        pronouncer.stop()
    }
}
