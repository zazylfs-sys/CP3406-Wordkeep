package au.edu.jcu.wordkeep.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.wordkeep.data.repository.StatsRepository
import au.edu.jcu.wordkeep.domain.stats.LearningStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Ready(val stats: LearningStats, val today: LocalDate) : StatsUiState
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: StatsRepository,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)

    val uiState: StateFlow<StatsUiState> = statsRepository.observeStats(today)
        .map<LearningStats, StatsUiState> { StatsUiState.Ready(it, today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)
}
