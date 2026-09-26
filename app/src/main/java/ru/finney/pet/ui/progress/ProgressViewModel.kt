package ru.finney.pet.ui.progress

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
import ru.finney.pet.domain.game.GoalProgress
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.EntryType
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.LedgerEntry
import ru.finney.pet.domain.model.SavedGame
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskTheme

/** Строка истории: откуда деньги или куда ушли — словами, и сколько. */
data class HistoryRow(val label: String, val balanceDelta: Int, val savingsDelta: Int)

data class HistoryPeriod(val number: Int, val rows: List<HistoryRow>)

/** Задание, которое пробовали: лучший исход и сколько было попыток. */
data class TaskRow(val title: String, val theme: String, val passed: Boolean, val attempts: Int)

sealed interface ProgressUiState {
    data object Loading : ProgressUiState

    data class Ready(
        val level: Int,
        val stage: Int,
        val points: Int,
        val goal: GoalProgress?,
        val goalsCompleted: Int,
        /** Последний закрытый период — для кнопки итогов. null — закрытых ещё нет. */
        val lastClosedPeriod: Int?,
        val tasks: List<TaskRow>,
        /** Новые периоды сверху. */
        val history: List<HistoryPeriod>,
    ) : ProgressUiState
}

/**
 * История и учебный прогресс — ТЗ п. 2.5.4 и 2.5.11: у каждого начисления и траты
 * видны источник и сумма, рядом пройденные задания, цель и итоги последнего периода.
 */
class ProgressViewModel(
    session: Session,
    private val game: Game,
    private val content: GameContent,
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = session.activeGame
        .filterNotNull()
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState.Loading)

    private fun toUiState(saved: SavedGame): ProgressUiState.Ready {
        val state = saved.state
        return ProgressUiState.Ready(
            level = game.level(state),
            stage = game.stage(state),
            points = state.points,
            goal = game.goalProgress(state),
            goalsCompleted = state.ledger.count { it.type == EntryType.GOAL_COMPLETE },
            lastClosedPeriod = state.periods.lastOrNull { it.result != null }?.number,
            tasks = state.attempts
                .groupBy { it.taskId }
                .mapNotNull { (taskId, attempts) ->
                    val task = content.task(taskId) ?: return@mapNotNull null
                    TaskRow(
                        title = task.title,
                        theme = task.theme.label(),
                        passed = attempts.any { it.outcome == TaskOutcome.SUCCESS },
                        attempts = attempts.size,
                    )
                },
            history = state.ledger
                .groupBy { it.periodNumber }
                .toSortedMap(compareByDescending { it })
                .map { (number, entries) ->
                    HistoryPeriod(
                        number = number,
                        // Лента в порядке записи; новые сверху. По времени не сортируем:
                        // у операций одной команды оно совпадает.
                        rows = entries.asReversed().map {
                            HistoryRow(content.entryLabel(it), it.balanceDelta, it.savingsDelta)
                        },
                    )
                },
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer().let { ProgressViewModel(it.session, it.game, it.content) } }
        }
    }
}

/** Подпись операции для ребёнка: источник денег или на что они ушли (ТЗ п. 2.5.4). */
fun GameContent.entryLabel(entry: LedgerEntry): String = when (entry.type) {
    EntryType.INCOME -> "Доход периода"
    EntryType.TASK_REWARD -> "Награда за мини-игру" +
        (entry.taskId?.let(::task)?.let { " «${it.title}»" } ?: "")
    EntryType.PARENT_BONUS -> "Бонус от взрослого"
    EntryType.PURCHASE -> {
        val item = entry.itemId?.let(::item)
        val kind = when (item?.category) {
            Category.NEEDS -> "нужное"
            Category.WANTS -> "хочется"
            null -> null
        }
        "Покупка: ${item?.label ?: "предмет"}" + (kind?.let { " ($it)" } ?: "")
    }
    EntryType.SAVINGS_DEPOSIT -> "В копилку" + (entry.goalId?.let(::goal)?.let { " на «${it.label}»" } ?: "")
    EntryType.SAVINGS_WITHDRAW -> "Из копилки" + (entry.goalId?.let(::goal)?.let { " с «${it.label}»" } ?: "")
    EntryType.GOAL_COMPLETE -> "Цель достигнута" + (entry.goalId?.let(::goal)?.let { ": ${it.label}" } ?: "")
}

fun TaskTheme.label(): String = when (this) {
    TaskTheme.PLANNING -> "планирование бюджета"
    TaskTheme.SAVINGS -> "сбережения"
    TaskTheme.SHOPPING -> "покупки и платежи"
}
