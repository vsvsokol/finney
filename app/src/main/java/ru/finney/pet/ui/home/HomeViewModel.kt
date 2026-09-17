package ru.finney.pet.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.GoalProgress
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.pet.Emotion

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** Всё, что ТЗ п. 2.5.3 требует показать на главном одновременно. */
    data class Ready(
        val petName: String,
        val appearance: PetAppearance,
        val isDemo: Boolean,
        val stats: PetStats,
        val emotion: Emotion,
        val level: Int,
        val stage: Int,
        val balance: Int,
        val totalSavings: Int,
        /** null — цель не выбрана. */
        val goal: GoalProgress?,
        val periodNumber: Int,
        val phase: PeriodPhase,
        /** Сколько стоит закрыть нужное при текущих шкалах. */
        val needsHint: Int?,
        /** Первое открытое и ещё не пройденное задание. null — всё пройдено. */
        val nextTask: TaskDefinition?,
    ) : HomeUiState {
        /** Период закрывается только после подтверждения плана. */
        val canClosePeriod: Boolean get() = phase == PeriodPhase.ACTIVE
    }
}

sealed interface HomeEvent {
    /** Открыть итоги: `PeriodResultRoute(periodNumber)`. */
    data class PeriodClosed(val periodNumber: Int) : HomeEvent

    data class Rejected(val reason: Rejection) : HomeEvent
}

class HomeViewModel(
    private val session: Session,
    private val game: Game,
    private val content: GameContent,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = session.activeGame
        .filterNotNull()
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun closePeriod() {
        viewModelScope.launch {
            val event = when (val result = session.execute { closePeriod(it) }) {
                is GameResult.Ok -> HomeEvent.PeriodClosed(result.state.periods.dropLast(1).last().number)
                is GameResult.Rejected -> HomeEvent.Rejected(result.reason)
            }
            _events.send(event)
        }
    }

    private fun toUiState(saved: SavedGame): HomeUiState.Ready {
        val state = saved.state
        val passed = state.attempts.filter { it.outcome == TaskOutcome.SUCCESS }.map { it.taskId }.toSet()
        return HomeUiState.Ready(
            petName = saved.profile.petName,
            appearance = saved.profile.appearance,
            isDemo = state.isDemo,
            stats = state.pet,
            emotion = game.emotion(state),
            level = game.level(state),
            stage = game.stage(state),
            balance = state.balance,
            totalSavings = state.totalSavings,
            goal = game.goalProgress(state),
            periodNumber = state.currentPeriod.number,
            phase = state.currentPeriod.phase,
            needsHint = game.needsHint(state),
            nextTask = content.tasks.firstOrNull { it.id !in passed && game.isTaskAvailable(state, it) },
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { HomeViewModel(it.session, it.game, it.content) } }
        }
    }
}
