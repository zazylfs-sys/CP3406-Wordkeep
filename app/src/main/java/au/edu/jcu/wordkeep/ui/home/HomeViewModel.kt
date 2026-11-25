package au.edu.jcu.wordkeep.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.wordkeep.data.repository.LookupResult
import au.edu.jcu.wordkeep.data.repository.StudyRepository
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.domain.model.MasteryLevel
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.domain.quiz.MeaningQuizBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.MIN,
    val dueCount: Int = 0,
    val newAvailable: Int = 0,
    val answeredToday: Int = 0,
    val totalWords: Int = 0,
    val readyWords: Int = 0,
    val masteredWords: Int = 0,
    val nextReview: LocalDate? = null,
    val pendingDownloads: Int = 0,
    val download: DownloadUi = DownloadUi.Idle,
    val offlineMode: Boolean = false,
) {
    val hasSessionWork: Boolean get() = dueCount + newAvailable > 0
    val canQuiz: Boolean get() = readyWords >= MeaningQuizBuilder.MIN_OPTIONS
    val canSpell: Boolean get() = readyWords > 0
}

sealed interface DownloadUi {
    data object Idle : DownloadUi
    data class Running(val done: Int, val total: Int) : DownloadUi
    data class Stopped(val reason: LookupResult.Reason) : DownloadUi
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    /** "Today" is refreshed whenever the screen resumes, so the desk is right after midnight. */
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val download = MutableStateFlow<DownloadUi>(DownloadUi.Idle)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = today.flatMapLatest { date ->
        combine(
            wordRepository.observeLibrary(),
            studyRepository.observeAnsweredToday(date),
            settingsRepository.settings,
            wordRepository.observePendingCount(),
            download,
        ) { words, answeredToday, settings, pending, downloadState ->
            buildState(date, words, answeredToday, settings.dailyNewWords, settings.offlineMode, pending, downloadState)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshDate() {
        today.value = LocalDate.now(clock)
    }

    init {
        // Finish an interrupted starter-pack download when the app opens, unless offline mode is on.
        viewModelScope.launch {
            if (wordRepository.observePendingCount().first() > 0 &&
                !settingsRepository.settings.first().offlineMode
            ) {
                downloadPending()
            }
        }
    }

    fun downloadPending() {
        if (download.value is DownloadUi.Running) return
        viewModelScope.launch {
            download.value = DownloadUi.Running(0, 0)
            val summary = wordRepository.downloadPending { done, total ->
                download.value = DownloadUi.Running(done, total)
            }
            download.value = summary.stoppedReason?.let { DownloadUi.Stopped(it) } ?: DownloadUi.Idle
        }
    }

    private fun buildState(
        today: LocalDate,
        words: List<StudyWord>,
        answeredToday: Int,
        dailyNewWords: Int,
        offlineMode: Boolean,
        pending: Int,
        downloadState: DownloadUi,
    ): HomeUiState {
        val ready = words.filter { it.word.isReady }
        val introducedToday = ready.count { it.review.introducedOn == today }
        val newReady = ready.count { it.review.isNew }
        return HomeUiState(
            loading = false,
            today = today,
            dueCount = ready.count { it.review.isDue(today) },
            newAvailable = minOf(newReady, (dailyNewWords - introducedToday).coerceAtLeast(0)),
            answeredToday = answeredToday,
            totalWords = words.size,
            readyWords = ready.size,
            masteredWords = ready.count { it.review.mastery == MasteryLevel.MASTERED },
            nextReview = ready.mapNotNull { it.review.dueDate }.filter { it.isAfter(today) }.minOrNull(),
            pendingDownloads = pending,
            download = downloadState,
            offlineMode = offlineMode,
        )
    }
}
