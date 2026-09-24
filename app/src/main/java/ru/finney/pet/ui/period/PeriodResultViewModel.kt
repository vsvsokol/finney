package ru.finney.pet.ui.period

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.finney.pet.appContainer
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.domain.model.SavedGame

sealed interface PeriodResultUiState {
    data object Loading : PeriodResultUiState

    /**
     * Периода с таким номером нет или он ещё не закрыт. Бывает после возврата из бэкапа:
     * маршрут в стеке остался, а состояние приехало другое.
     */
    data object Unavailable : PeriodResultUiState

    /** Итоги закрытого периода: что планировали, что вышло и за что дали очки (ТЗ п. 2.5.9). */
    data class Ready(
        val periodNumber: Int,
        val plan: Plan,
        val facts: PeriodFacts,
        /** Три условия очков за период — показываем каждое отдельной строкой, а не одним итогом. */
        val needsCovered: Boolean,
        val planMatched: Boolean,
        val savingsAdded: Boolean,
        val successfulTasks: Int,
        val pointsEarned: Int,
        val totalPoints: Int,
        val level: Int,
        /** Уровень вырос именно за этот период. */
        val leveledUp: Boolean,
        /** Названия игр, которые с новым уровнем стали сложнее или открылись. */
        val harderGames: List<String>,
        val balance: Int,
        /** Период уже открыт следующим: закрытие периода сразу начинает новый. */
        val nextPeriodNumber: Int,
    ) : PeriodResultUiState {
        /** Перерасход по нужному и желаемому сверх плана; 0, если уложились. */
        val overspend: Int
            get() = (facts.needs - plan.needs).coerceAtLeast(0) + (facts.wants - plan.wants).coerceAtLeast(0)
    }
}

/**
 * Экран открывается по `PeriodResultRoute(periodNumber)` сразу после закрытия периода,
 * поэтому номер приходит маршрутом, а не берётся из текущего состояния: пока ребёнок
 * читает итоги, текущим уже стал следующий период.
 */
class PeriodResultViewModel(
    private val periodNumber: Int,
    session: Session,
    private val game: Game,
) : ViewModel() {

    val uiState: StateFlow<PeriodResultUiState> = session.activeGame
        .filterNotNull()
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodResultUiState.Loading)

    private fun toUiState(saved: SavedGame): PeriodResultUiState {
        val state = saved.state
        val period = state.periods.firstOrNull { it.number == periodNumber }
        val plan = period?.plan
        val result = period?.result
        if (plan == null || result == null) return PeriodResultUiState.Unavailable

        val totalPoints = state.points
        return PeriodResultUiState.Ready(
            periodNumber = period.number,
            plan = plan,
            facts = result.facts,
            needsCovered = result.needsCovered,
            planMatched = result.planMatched,
            savingsAdded = result.savingsAdded,
            successfulTasks = result.successfulTasks,
            pointsEarned = result.points,
            totalPoints = totalPoints,
            level = game.levelFor(totalPoints),
            leveledUp = game.levelFor(totalPoints) > game.levelFor(totalPoints - result.points),
            harderGames = game.tasksUnlockedBetween(game.levelFor(totalPoints - result.points), game.levelFor(totalPoints))
                .map { it.title }
                .distinct(),
            balance = state.balance,
            nextPeriodNumber = state.currentPeriod.number,
        )
    }

    companion object {
        fun factory(periodNumber: Int) = viewModelFactory {
            initializer { appContainer().let { PeriodResultViewModel(periodNumber, it.session, it.game) } }
        }
    }
}
