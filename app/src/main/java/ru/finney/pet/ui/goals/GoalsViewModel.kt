package ru.finney.pet.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.GoalProgress
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.SavedGame

/** Цель в списке выбора: цена, накопленное и то, что с ней уже всё. */
data class GoalRow(
    val goal: Goal,
    val saved: Int,
    val isActive: Boolean,
    val isCompleted: Boolean,
)

sealed interface GoalsUiState {
    data object Loading : GoalsUiState

    data class Ready(
        val goals: List<GoalRow>,
        /** null — цель не выбрана: копить пока некуда. */
        val progress: GoalProgress?,
        val balance: Int,
        /** Пополнять и снимать можно только после подтверждения плана. */
        val canMoveMoney: Boolean,
        /** Копилка цели набрана: можно подтвердить достижение. */
        val canComplete: Boolean,
        val rejection: Rejection?,
    ) : GoalsUiState
}

/**
 * Цели и копилка. Копилка в плане периода списывается сразу при подтверждении —
 * здесь ребёнок выбирает цель заранее и при желании докладывает сверх плана.
 */
class GoalsViewModel(
    private val session: Session,
    private val game: Game,
    private val content: GameContent,
) : ViewModel() {

    private val rejection = MutableStateFlow<Rejection?>(null)

    val uiState: StateFlow<GoalsUiState> =
        combine(session.activeGame.filterNotNull(), rejection, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState.Loading)

    fun selectGoal(goalId: String) = run { session.execute { selectGoal(it, goalId) } }

    fun deposit(amount: Int) = run { session.execute { deposit(it, amount) } }

    fun withdraw(amount: Int) = run { session.execute { withdraw(it, amount) } }

    fun completeGoal() = run { session.execute { completeGoal(it) } }

    private fun run(command: suspend () -> GameResult) {
        viewModelScope.launch {
            rejection.value = (command() as? GameResult.Rejected)?.reason
        }
    }

    private fun toUiState(saved: SavedGame, rejection: Rejection?): GoalsUiState {
        val state = saved.state
        val progress = game.goalProgress(state)
        return GoalsUiState.Ready(
            goals = content.goals.map { goal ->
                GoalRow(
                    goal = goal,
                    saved = state.goalSaved(goal.id),
                    isActive = goal.id == state.activeGoalId,
                    isCompleted = state.isGoalCompleted(goal.id),
                )
            },
            progress = progress,
            balance = state.balance,
            canMoveMoney = state.currentPeriod.phase == PeriodPhase.ACTIVE,
            canComplete = progress != null && progress.remaining <= 0,
            rejection = rejection,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { GoalsViewModel(it.session, it.game, it.content) } }
        }
    }
}
