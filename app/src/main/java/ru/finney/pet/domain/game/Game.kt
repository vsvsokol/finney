package ru.finney.pet.domain.game

import ru.finney.pet.domain.economy.SavingsRules
import ru.finney.pet.domain.model.EntryType
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.domain.model.ItemKind
import ru.finney.pet.domain.model.LedgerEntry
import ru.finney.pet.domain.model.Period
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.PeriodResult
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.domain.model.ShopItem
import ru.finney.pet.domain.model.StatEffect
import ru.finney.pet.domain.model.TaskAttempt
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.period.PeriodRules
import ru.finney.pet.domain.pet.Emotion
import ru.finney.pet.domain.pet.PetRules
import ru.finney.pet.domain.progress.Progression
import ru.finney.pet.domain.tasks.TaskDetails
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskEvaluation
import ru.finney.pet.domain.tasks.TaskInput

data class GoalProgress(
    val goal: Goal,
    val saved: Int,
    val remaining: Int,
    /** null — срок пока не посчитать: пополнений не было. */
    val periodsToGoal: Int?,
    val averageDeposit: Int,
)

data class PurchasePreview(
    val item: ShopItem,
    val statsBefore: PetStats,
    val statsAfter: PetStats,
    /** 0 — денег хватает. */
    val shortage: Int,
)

data class WithdrawPreview(
    val savedBefore: Int,
    val savedAfter: Int,
    val periodsBefore: Int?,
    val periodsAfter: Int?,
)

sealed interface TaskResult {
    /** Исход задания и числа для экрана объяснения. [reward] = 0, если награда уже выдавалась. */
    data class Submitted(
        val state: GameState,
        val outcome: TaskOutcome,
        val details: TaskDetails,
        val reward: Int,
    ) : TaskResult

    /** Ввод не принят или задание недоступно. Состояние не изменилось. */
    data class Rejected(val reason: Rejection) : TaskResult
}

/**
 * Единственная точка изменения игрового состояния. Чистые функции: состояние на входе,
 * новое состояние или отказ на выходе. Правила — docs/economy.md.
 */
class Game(
    private val content: GameContent,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val economy get() = content.economy

    fun newGame(isDemo: Boolean = false): GameState = openPeriod(
        GameState(
            isDemo = isDemo,
            pet = economy.pet.start,
            activeGoalId = null,
            periods = emptyList(),
            ledger = emptyList(),
            attempts = emptyList(),
        ),
    )

    // ---------- Чтение ----------

    fun level(state: GameState): Int = Progression.level(state.points, economy)

    fun stage(state: GameState): Int = Progression.stage(level(state), economy)

    fun emotion(state: GameState): Emotion = PetRules.emotion(state.pet, economy.pet)

    /** Сколько минимально стоит закрыть нужное при текущих шкалах. */
    fun needsHint(state: GameState): Int? =
        PetRules.needsCost(state.pet, content.shop, economy.pet.needsThreshold)

    fun isTaskAvailable(state: GameState, task: TaskDefinition): Boolean =
        state.isDemo || task.unlockPeriod <= state.currentPeriod.number

    fun goalProgress(state: GameState, goalId: String? = state.activeGoalId): GoalProgress? {
        val goal = goalId?.let(content::goal) ?: return null
        val saved = state.goalSaved(goal.id)
        val remaining = maxOf(0, goal.price - saved)
        val pace = SavingsRules.pace(state.periods)
        return GoalProgress(goal, saved, remaining, SavingsRules.periodsToGoal(remaining, pace), pace.average)
    }

    fun previewPurchase(state: GameState, itemId: String): PurchasePreview? {
        val item = content.item(itemId) ?: return null
        return PurchasePreview(
            item = item,
            statsBefore = state.pet,
            statsAfter = PetRules.apply(state.pet, item.effect),
            shortage = maxOf(0, item.price - state.balance),
        )
    }

    fun previewWithdraw(state: GameState, amount: Int): WithdrawPreview? {
        val goal = state.activeGoalId?.let(content::goal) ?: return null
        val saved = state.goalSaved(goal.id)
        val savedAfter = maxOf(0, saved - amount)
        val pace = SavingsRules.pace(state.periods)
        return WithdrawPreview(
            savedBefore = saved,
            savedAfter = savedAfter,
            periodsBefore = SavingsRules.periodsToGoal(goal.price - saved, pace),
            periodsAfter = SavingsRules.periodsToGoal(goal.price - savedAfter, pace),
        )
    }

    // ---------- План ----------

    fun confirmPlan(state: GameState, needs: Int, wants: Int, savings: Int): GameResult {
        val period = state.currentPeriod
        if (period.phase != PeriodPhase.PLANNING) return reject(Rejection.PlanAlreadyConfirmed)
        if (needs < 0 || wants < 0 || savings < 0) return reject(Rejection.InvalidAmount)
        val plan = Plan(budget = state.balance, needs = needs, wants = wants, savings = savings)
        if (plan.remainder < 0) return reject(Rejection.PlanExceedsBudget(plan.budget, plan.planned))
        return ok(state.withCurrentPeriod(period.copy(phase = PeriodPhase.ACTIVE, plan = plan)))
    }

    // ---------- Покупки ----------

    fun buy(state: GameState, itemId: String): GameResult {
        val item = content.item(itemId) ?: return reject(Rejection.UnknownItem(itemId))
        if (state.currentPeriod.phase != PeriodPhase.ACTIVE) return reject(Rejection.PlanNotConfirmed)
        if (item.kind == ItemKind.ACCESSORY && state.owns(item.id)) return reject(Rejection.AlreadyOwned)
        if (item.price > state.balance) return reject(Rejection.InsufficientFunds(item.price, state.balance))
        val entry = entry(state, EntryType.PURCHASE, balanceDelta = -item.price)
            .copy(category = item.category, itemId = item.id)
        return ok(state.copy(pet = PetRules.apply(state.pet, item.effect), ledger = state.ledger + entry))
    }

    // ---------- Накопления ----------

    fun selectGoal(state: GameState, goalId: String): GameResult {
        if (content.goal(goalId) == null) return reject(Rejection.UnknownGoal(goalId))
        if (state.isGoalCompleted(goalId)) return reject(Rejection.GoalAlreadyCompleted)
        return ok(state.copy(activeGoalId = goalId))
    }

    fun deposit(state: GameState, amount: Int): GameResult {
        if (state.currentPeriod.phase != PeriodPhase.ACTIVE) return reject(Rejection.PlanNotConfirmed)
        if (amount <= 0) return reject(Rejection.InvalidAmount)
        val goalId = state.activeGoalId ?: return reject(Rejection.NoActiveGoal)
        if (amount > state.balance) return reject(Rejection.InsufficientFunds(amount, state.balance))
        val entry = entry(state, EntryType.SAVINGS_DEPOSIT, balanceDelta = -amount, savingsDelta = amount)
            .copy(goalId = goalId)
        return ok(state.copy(ledger = state.ledger + entry))
    }

    /** UI обязан показать [previewWithdraw] и получить отдельное подтверждение (ТЗ п. 2.5.7). */
    fun withdraw(state: GameState, amount: Int): GameResult {
        if (state.currentPeriod.phase != PeriodPhase.ACTIVE) return reject(Rejection.PlanNotConfirmed)
        if (amount <= 0) return reject(Rejection.InvalidAmount)
        val goalId = state.activeGoalId ?: return reject(Rejection.NoActiveGoal)
        val saved = state.goalSaved(goalId)
        if (amount > saved) return reject(Rejection.InsufficientSavings(amount, saved))
        val entry = entry(state, EntryType.SAVINGS_WITHDRAW, balanceDelta = amount, savingsDelta = -amount)
            .copy(goalId = goalId)
        return ok(state.copy(ledger = state.ledger + entry))
    }

    fun completeGoal(state: GameState): GameResult {
        val goal = state.activeGoalId?.let(content::goal) ?: return reject(Rejection.NoActiveGoal)
        val saved = state.goalSaved(goal.id)
        if (saved < goal.price) return reject(Rejection.GoalNotReached(goal.price, saved))
        val entry = entry(state, EntryType.GOAL_COMPLETE, balanceDelta = 0, savingsDelta = -goal.price)
            .copy(goalId = goal.id)
        return ok(
            state.copy(
                pet = PetRules.apply(state.pet, StatEffect(mood = goal.moodBonus)),
                activeGoalId = null,
                ledger = state.ledger + entry,
            ),
        )
    }

    // ---------- Доход ----------

    fun addParentBonus(state: GameState, amount: Int): GameResult {
        val rule = economy.parentBonus
        if (amount <= 0) return reject(Rejection.InvalidAmount)
        if (amount % rule.step != 0) return reject(Rejection.BonusNotOnStep(rule.step))
        val given = state.ledger
            .filter { it.type == EntryType.PARENT_BONUS && it.periodNumber == state.currentPeriod.number }
            .sumOf { it.balanceDelta }
        val left = rule.maxPerPeriod - given
        if (amount > left) return reject(Rejection.BonusLimitExceeded(left))
        return ok(state.copy(ledger = state.ledger + income(state, EntryType.PARENT_BONUS, amount)))
    }

    fun submitTask(state: GameState, taskId: String, input: TaskInput): TaskResult {
        val task = content.task(taskId) ?: return TaskResult.Rejected(Rejection.UnknownTask(taskId))
        if (!isTaskAvailable(state, task)) return TaskResult.Rejected(Rejection.TaskLocked)
        val evaluation = when (val e = TaskEngines.evaluate(task, input)) {
            is TaskEvaluation.Invalid -> return TaskResult.Rejected(Rejection.InvalidTaskInput(e.error))
            is TaskEvaluation.Done -> e
        }

        val previous = state.attempts.filter { it.taskId == taskId }
        val rates = task.reward ?: economy.taskReward
        val reward = when (evaluation.outcome) {
            TaskOutcome.SUCCESS -> if (previous.none { it.outcome == TaskOutcome.SUCCESS }) rates.success else 0
            TaskOutcome.FAIL -> if (previous.isEmpty()) rates.fail else 0
        }
        val attempt = TaskAttempt(state.currentPeriod.number, taskId, evaluation.outcome, reward, clock())
        val ledger = if (reward > 0) {
            state.ledger + income(state, EntryType.TASK_REWARD, reward).copy(taskId = taskId)
        } else {
            state.ledger
        }
        return TaskResult.Submitted(
            state = state.copy(ledger = ledger, attempts = state.attempts + attempt),
            outcome = evaluation.outcome,
            details = evaluation.details,
            reward = reward,
        )
    }

    // ---------- Период ----------

    /** Итоги закрытого периода — `periods[size - 2].result` в новом состоянии. */
    fun closePeriod(state: GameState): GameResult {
        val period = state.currentPeriod
        val plan = period.plan
        if (period.phase != PeriodPhase.ACTIVE || plan == null) return reject(Rejection.PlanNotConfirmed)

        val facts = PeriodRules.facts(state.ledger, period.number)
        val needsCovered = PetRules.needsCovered(state.pet, economy.pet)
        val planMatched = PeriodRules.planMatched(plan, facts, economy.planTolerance)
        val savingsAdded = facts.savings > 0
        val successfulTasks = firstSuccessesIn(state, period.number)
        val result = PeriodResult(
            facts = facts,
            needsCovered = needsCovered,
            planMatched = planMatched,
            savingsAdded = savingsAdded,
            successfulTasks = successfulTasks,
            points = Progression.periodPoints(needsCovered, planMatched, savingsAdded, successfulTasks, economy.points),
        )
        val closed = state.withCurrentPeriod(period.copy(phase = PeriodPhase.CLOSED, result = result))
        return ok(openPeriod(closed))
    }

    /** Задания, впервые пройденные успешно именно в этом периоде — повтор очков не даёт. */
    private fun firstSuccessesIn(state: GameState, periodNumber: Int): Int =
        state.attempts
            .filter { it.outcome == TaskOutcome.SUCCESS }
            .groupBy { it.taskId }
            .count { (_, attempts) -> attempts.first().periodNumber == periodNumber }

    private fun openPeriod(state: GameState): GameState {
        val number = state.periods.size + 1
        val stage = stage(state)
        val opened = state.copy(
            pet = PetRules.decay(state.pet, economy.decay(stage)),
            periods = state.periods + Period(number = number, stage = stage, phase = PeriodPhase.PLANNING),
        )
        val income = entry(opened, EntryType.INCOME, balanceDelta = economy.income(stage))
        return opened.copy(ledger = opened.ledger + income)
    }

    // ---------- Служебное ----------

    private fun entry(state: GameState, type: EntryType, balanceDelta: Int, savingsDelta: Int = 0) = LedgerEntry(
        periodNumber = state.currentPeriod.number,
        type = type,
        balanceDelta = balanceDelta,
        savingsDelta = savingsDelta,
        createdAt = clock(),
    )

    /** Доход после подтверждения плана — незапланированный. */
    private fun income(state: GameState, type: EntryType, amount: Int) =
        entry(state, type, balanceDelta = amount).copy(unplanned = state.currentPeriod.phase == PeriodPhase.ACTIVE)

    private fun GameState.withCurrentPeriod(period: Period) = copy(periods = periods.dropLast(1) + period)

    private fun ok(state: GameState) = GameResult.Ok(state)

    private fun reject(reason: Rejection) = GameResult.Rejected(reason)
}
