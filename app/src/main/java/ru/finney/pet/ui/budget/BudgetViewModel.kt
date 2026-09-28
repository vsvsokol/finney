package ru.finney.pet.ui.budget

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
import ru.finney.pet.domain.game.PlanReport
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.domain.model.SavedGame

sealed interface BudgetUiState {
    data object Loading : BudgetUiState

    /** План не подтверждён: ребёнок распределяет бюджет, суммы можно менять сколько угодно. */
    data class Planning(
        val periodNumber: Int,
        /** Уровень, который начинается с этого плана. */
        val level: Int,
        /** Всё, что есть на балансе сейчас: доход периода, остаток прошлых, награды до плана. */
        val budget: Int,
        val needs: Int,
        val wants: Int,
        val savings: Int,
        /** Подсказка: сколько минимум заложить на нужное. */
        val needsHint: Int?,
        /** null — цель не выбрана, откладывать пока некуда. */
        val goalLabel: String?,
        /** Цели на выбор прямо в плане, если своей ещё нет: без цели копилку не заполнить. */
        val goals: List<Goal> = emptyList(),
        /** Сколько направлений должно быть заполнено; из economy.json. */
        val directions: Int = 3,
        val rejection: Rejection?,
        val isSaving: Boolean,
    ) : BudgetUiState {
        val planned: Int get() = needs + wants + savings
        val remainder: Int get() = budget - planned
        /** ТЗ п. 2.5.5: сумма разложена минимум по трём направлениям — в каждом хоть что-то. */
        val allDirections: Boolean get() = listOf(needs, wants, savings).count { it > 0 } >= directions
        val canConfirm: Boolean get() = remainder >= 0 && allDirections && !isSaving

        /**
         * Чего не хватает, чтобы подтвердить, — пунктами для подписи у погашенной кнопки.
         * Плейтест: серую кнопку жали и не понимали, что не так. Пусто — подтверждать можно.
         */
        val missing: List<String> get() = buildList {
            if (remainder < 0) add("Убавь: разложено больше, чем есть")
            if (needs == 0) add("Положи монетки в «Нужное»")
            if (wants == 0) add("Положи монетки в «Желаемое»")
            if (goalLabel == null) add("Выбери цель для копилки") else if (savings == 0) add("Положи монетки в копилку")
        }
    }

    /** План подтверждён: план против факта до закрытия периода. */
    data class Active(
        val periodNumber: Int,
        val level: Int,
        val report: PlanReport,
        val balance: Int,
    ) : BudgetUiState
}

class BudgetViewModel(
    private val session: Session,
    private val game: Game,
    private val content: GameContent,
) : ViewModel() {

    private data class Draft(
        val needs: Int = 0,
        val wants: Int = 0,
        val savings: Int = 0,
        val rejection: Rejection? = null,
        val isSaving: Boolean = false,
    )

    private val draft = MutableStateFlow(Draft())

    val uiState: StateFlow<BudgetUiState> = combine(session.activeGame.filterNotNull(), draft, ::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState.Loading)

    fun setNeeds(amount: Int) = draft.update { it.copy(needs = amount.coerceAtLeast(0), rejection = null) }

    fun setWants(amount: Int) = draft.update { it.copy(wants = amount.coerceAtLeast(0), rejection = null) }

    fun setSavings(amount: Int) = draft.update { it.copy(savings = amount.coerceAtLeast(0), rejection = null) }

    fun selectGoal(goalId: String) {
        viewModelScope.launch {
            val result = session.execute { selectGoal(it, goalId) }
            if (result is GameResult.Rejected) draft.update { it.copy(rejection = result.reason) }
        }
    }

    /** После успеха экран сам переключится на [BudgetUiState.Active]. */
    fun confirm() {
        val current = draft.value
        if (current.isSaving) return
        draft.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = session.execute { confirmPlan(it, current.needs, current.wants, current.savings) }
            draft.update {
                when (result) {
                    // Черновик больше не нужен; следующий период начнётся с нулей.
                    is GameResult.Ok -> Draft()
                    is GameResult.Rejected -> it.copy(rejection = result.reason, isSaving = false)
                }
            }
        }
    }

    private fun toUiState(saved: SavedGame, draft: Draft): BudgetUiState {
        val state = saved.state
        val report = game.planReport(state)
        if (report != null) {
            return BudgetUiState.Active(state.currentPeriod.number, game.level(state), report, state.balance)
        }
        return BudgetUiState.Planning(
            periodNumber = state.currentPeriod.number,
            level = game.level(state),
            budget = state.balance,
            needs = draft.needs,
            wants = draft.wants,
            savings = draft.savings,
            needsHint = game.needsHint(state),
            goalLabel = game.goalProgress(state)?.goal?.label,
            goals = if (state.activeGoalId == null) {
                content.goals.filterNot { state.isGoalCompleted(it.id) }
            } else {
                emptyList()
            },
            directions = content.economy.planDirections,
            rejection = draft.rejection,
            isSaving = draft.isSaving,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { BudgetViewModel(it.session, it.game, it.content) } }
        }
    }
}
