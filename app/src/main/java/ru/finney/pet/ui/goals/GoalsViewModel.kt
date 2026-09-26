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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.GoalProgress
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.game.WithdrawPreview
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.ui.components.ActionFeedback
import ru.finney.pet.ui.components.changesBetween

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
        /** Итог последнего взноса или снятия: что изменилось и что дальше. */
        val feedback: ActionFeedback? = null,
        /** Снятие ждёт подтверждения: как изменятся копилка и срок (ТЗ п. 2.5.7). */
        val pendingWithdraw: PendingWithdraw? = null,
    ) : GoalsUiState
}

/** Снятие, которое ещё не подтвердили. */
data class PendingWithdraw(val amount: Int, val preview: WithdrawPreview)

/** Что происходит на экране поверх данных игры. */
private data class GoalsScreenState(
    val rejection: Rejection? = null,
    val feedback: ActionFeedback? = null,
    val pendingWithdraw: PendingWithdraw? = null,
)

/**
 * Цели и копилка. Копилка в плане периода списывается сразу при подтверждении —
 * здесь ребёнок выбирает цель заранее и при желании докладывает сверх плана.
 */
class GoalsViewModel(
    private val session: Session,
    private val game: Game,
    private val content: GameContent,
) : ViewModel() {

    private val screen = MutableStateFlow(GoalsScreenState())

    val uiState: StateFlow<GoalsUiState> =
        combine(session.activeGame.filterNotNull(), screen, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState.Loading)

    fun selectGoal(goalId: String) = run { session.execute { selectGoal(it, goalId) } }

    fun deposit(amount: Int) = run(
        feedback = { before, after ->
            ActionFeedback(
                title = "Отложили $amount",
                lines = changesBetween(before, after),
                why = "Копилка растёт — цель ближе.",
                next = "Откладывай каждый период.",
            )
        },
    ) { session.execute { deposit(it, amount) } }

    /**
     * Первый шаг снятия: только показать, что будет. Сами деньги не двигаются,
     * пока ребёнок не нажмёт «Снять» в окне подтверждения (ТЗ п. 2.5.7).
     */
    fun askWithdraw(amount: Int) {
        viewModelScope.launch {
            val state = session.activeGame.first()?.state ?: return@launch
            val preview = game.previewWithdraw(state, amount)
            screen.value = if (preview == null) {
                GoalsScreenState(rejection = Rejection.NoActiveGoal)
            } else {
                GoalsScreenState(pendingWithdraw = PendingWithdraw(amount, preview))
            }
        }
    }

    fun cancelWithdraw() = screen.update { it.copy(pendingWithdraw = null) }

    fun confirmWithdraw() {
        val pending = screen.value.pendingWithdraw ?: return
        screen.update { it.copy(pendingWithdraw = null) }
        run(
            feedback = { before, after ->
                ActionFeedback(
                    title = "Сняли ${pending.amount} из копилки",
                    lines = changesBetween(before, after),
                    why = "До цели стало дальше.",
                    next = "Передумаешь — отложи снова.",
                )
            },
        ) { session.execute { withdraw(it, pending.amount) } }
    }

    fun completeGoal() = run(
        feedback = { before, after ->
            ActionFeedback(
                title = "Цель достигнута!",
                lines = changesBetween(before, after),
                why = "Ты копил — и получилось!",
                next = "Выбери новую цель.",
            )
        },
    ) { session.execute { completeGoal(it) } }

    fun dismissFeedback() = screen.update { it.copy(feedback = null, rejection = null) }

    private fun run(
        feedback: ((before: GameState, after: GameState) -> ActionFeedback)? = null,
        command: suspend () -> GameResult,
    ) {
        viewModelScope.launch {
            val before = session.activeGame.first()?.state
            screen.value = when (val result = command()) {
                is GameResult.Ok -> GoalsScreenState(
                    feedback = if (feedback != null && before != null) feedback(before, result.state) else null,
                )
                is GameResult.Rejected -> GoalsScreenState(rejection = result.reason)
            }
        }
    }

    private fun toUiState(saved: SavedGame, screen: GoalsScreenState): GoalsUiState {
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
            rejection = screen.rejection,
            feedback = screen.feedback,
            pendingWithdraw = screen.pendingWithdraw,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { GoalsViewModel(it.session, it.game, it.content) } }
        }
    }
}
