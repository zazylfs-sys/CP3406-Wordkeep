package au.edu.jcu.wordkeep.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.wordkeep.data.repository.PrivacyRepository
import au.edu.jcu.wordkeep.data.repository.StatsRepository
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.data.settings.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import au.edu.jcu.wordkeep.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}

data class YourDataUiState(
    val words: Int = 0,
    val answers: Int = 0,
    val offlineMode: Boolean = false,
)

sealed interface DataMessage {
    data class Exported(val words: Int) : DataMessage
    data object ExportFailed : DataMessage
    data object Deleted : DataMessage
}

@HiltViewModel
class YourDataViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privacyRepository: PrivacyRepository,
    statsRepository: StatsRepository,
    settingsRepository: SettingsRepository,
    clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    val uiState: StateFlow<YourDataUiState> =
        kotlinx.coroutines.flow.combine(statsRepository.observeStats(LocalDate.now(clock)), settingsRepository.settings) { stats, settings ->
            YourDataUiState(words = stats.mastery.total, answers = stats.totalAnswered, offlineMode = settings.offlineMode)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YourDataUiState())

    private val _message = MutableStateFlow<DataMessage?>(null)
    val message: StateFlow<DataMessage?> = _message.asStateFlow()

    /** Writes the CSV to a file the learner chose with the system file picker (no storage permission needed). */
    fun export(uri: Uri) {
        viewModelScope.launch {
            _message.value = try {
                val stream = context.contentResolver.openOutputStream(uri) ?: throw IllegalStateException("No stream")
                DataMessage.Exported(stream.use { privacyRepository.exportCsv(it) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DataMessage.ExportFailed
            }
        }
    }

    /**
     * The erase runs in the app scope so that leaving the screen cannot stop it halfway
     * (for example after Room is cleared but before the settings are).
     */
    fun deleteEverything() {
        viewModelScope.launch {
            appScope.launch { privacyRepository.deleteEverything() }.join()
            _message.value = DataMessage.Deleted
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
