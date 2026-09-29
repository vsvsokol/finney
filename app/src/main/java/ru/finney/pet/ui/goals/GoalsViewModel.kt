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
import ru.finney.pet.domain.model.EntryType
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
        /**
         * Окно с итогом — только для достигнутой цели: это событие, а не рутина. Экран
         * показывает из него вещь, заголовок и «почему», без строк «было → стало».
         */
        val feedback: ActionFeedback? = null,
        /** Итог последнего взноса или снятия словами — под кошельком и копилкой, без окон. */
        val note: String? = null,
        /**
         * На сколько сдвинулась копилка последним действием: звук, монеты летят между
         * кошельком и копилкой и «+15» / «−15» над ними.
         */
        val savingsMove: SavingsMove? = null,
        /** Последние взносы и снятия по этой цели, новые сверху: что положил и что взял. */
        val moves: List<SavingsEntry> = emptyList(),
        /** Снятие ждёт подтверждения: как изменятся копилка и срок (ТЗ п. 2.5.7). */
        val pendingWithdraw: PendingWithdraw? = null,
        /**
         * Сколько по плану этого уровня ещё отложить; 0 — план уже выполнен или его нет.
         * С этой суммы начинается «−/+»: одно нажатие «Положить» — и копилка по плану.
         */
        val planSavingsLeft: Int = 0,
        /** Первые уровни: рука показывает на «Положить», пока по плану не отложено. */
        val guide: Boolean = false,
    ) : GoalsUiState
}

/** Сдвиг копилки. [id] растёт с каждым действием: два одинаковых взноса подряд — два звука. */
data class SavingsMove(val id: Int, val delta: Int)

/** Строка истории копилки: плюс — положил, минус — взял. */
data class SavingsEntry(val delta: Int)

/** Снятие, которое ещё не подтвердили. */
data class PendingWithdraw(val amount: Int, val preview: WithdrawPreview)

/** Что происходит на экране поверх данных игры. */
private data class GoalsScreenState(
    val rejection: Rejection? = null,
    val feedback: ActionFeedback? = null,
    val note: String? = null,
    val pendingWithdraw: PendingWithdraw? = null,
    val savingsMove: SavingsMove? = null,
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
        note = { _, after -> "Отложили $amount. ${leftLine(after)}" },
    ) { session.execute { deposit(it, amount) } }

    private fun leftLine(state: GameState): String {
        val left = game.goalProgress(state)?.remaining ?: return ""
        return if (left > 0) "До цели осталось $left." else "Хватает — забирай!"
    }

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
            note = { _, after -> "Сняли ${pending.amount}. ${leftLine(after)}" },
        ) { session.execute { withdraw(it, pending.amount) } }
    }

    /**
     * Цель достигнута: вещь переходит питомцу и сразу надета. Итог — окном с самой
     * вещью и одной фразой: плейтест 29.09 назвал прежнее окно захламлённым.
     */
    fun completeGoal() = run(
        feedback = { before, after ->
            val goal = before.activeGoalId?.let(content::goal)
            ActionFeedback(
                title = "${goal?.label ?: "Цель"} — твоя!",
                lines = changesBetween(before, after),
                why = "Копил — и получилось! Она уже на питомце.",
                next = "Переодеть можно в гардеробе. Выбери новую цель.",
                itemId = goal?.reward,
            )
        },
    ) { session.execute { completeGoal(it) } }

    fun dismissFeedback() = screen.update { it.copy(feedback = null, rejection = null) }

    private fun run(
        feedback: ((before: GameState, after: GameState) -> ActionFeedback)? = null,
        note: ((before: GameState, after: GameState) -> String)? = null,
        command: suspend () -> GameResult,
    ) {
        viewModelScope.launch {
            val before = session.activeGame.first()?.state
            val moveId = (screen.value.savingsMove?.id ?: 0) + 1
            screen.value = when (val result = command()) {
                is GameResult.Ok -> GoalsScreenState(
                    feedback = if (feedback != null && before != null) feedback(before, result.state) else null,
                    note = if (note != null && before != null) note(before, result.state) else null,
                    savingsMove = before
                        ?.let { result.state.totalSavings - it.totalSavings }
                        ?.takeIf { it != 0 && note != null }
                        ?.let { SavingsMove(moveId, it) },
                )
                is GameResult.Rejected -> GoalsScreenState(rejection = result.reason)
            }
        }
    }

    private fun toUiState(saved: SavedGame, screen: GoalsScreenState): GoalsUiState {
        val state = saved.state
        val progress = game.goalProgress(state)
        val planSavingsLeft = game.planReport(state)
            ?.let { (it.plan.savings - it.facts.savings).coerceAtLeast(0) } ?: 0
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
            note = screen.note,
            pendingWithdraw = screen.pendingWithdraw,
            savingsMove = screen.savingsMove,
            moves = state.activeGoalId?.let { id ->
                state.ledger
                    .filter { it.goalId == id && (it.type == EntryType.SAVINGS_DEPOSIT || it.type == EntryType.SAVINGS_WITHDRAW) }
                    .takeLast(HISTORY_ROWS)
                    .reversed()
                    .map { SavingsEntry(it.savingsDelta) }
            }.orEmpty(),
            planSavingsLeft = planSavingsLeft,
            guide = game.level(state) <= GUIDED_LEVELS && planSavingsLeft > 0,
        )
    }

    companion object {
        /** До какого уровня рука показывает на «Положить» — как рука подсказки на главном. */
        private const val GUIDED_LEVELS = 3

        /** Сколько последних движений копилки видно: больше — это уже «Итоги и история». */
        private const val HISTORY_ROWS = 4

        val Factory = viewModelFactory {
            initializer { appContainer().let { GoalsViewModel(it.session, it.game, it.content) } }
        }
    }
}
