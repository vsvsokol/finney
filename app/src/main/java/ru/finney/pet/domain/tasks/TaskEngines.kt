package ru.finney.pet.domain.tasks

import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ChangeMode
import ru.finney.pet.domain.model.ChangeRound
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.DistributorRule
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome

sealed interface TaskInput {
    /** distributor: сумма по id корзины. Отсутствующая корзина = 0. */
    data class Distribution(val amounts: Map<String, Int>) : TaskInput

    /** basket: выбранные товары с полки. */
    data class Basket(val items: Set<String>) : TaskInput

    /** goal_slider: регулярный взнос. */
    data class Deposit(val amount: Int) : TaskInput

    /** sorter: куда ребёнок отнёс каждую вещь — первый выбор, до подсказки. */
    data class Sorting(val answers: Map<String, Category>) : TaskInput

    /** goal_race: сколько отложено в каждый день, по порядку. */
    data class DailyDeposits(val amounts: List<Int>) : TaskInput

    /** reserve: что взято в план недели и что перенесено ради непредвиденной траты. */
    data class Reserve(val planned: Set<String>, val dropped: Set<String>) : TaskInput

    /** stand: сколько штук сырья закуплено. */
    data class Stock(val count: Int) : TaskInput

    /** change: монеты каждого раунда с первой попытки, по порядку раундов. */
    data class Coins(val rounds: List<List<Int>>) : TaskInput
}

/** Ввод, который нельзя оценить. Это не исход задания: ребёнок исправляет и пробует снова. */
sealed interface TaskInputError {
    data object WrongInputType : TaskInputError
    data object NegativeAmount : TaskInputError
    data class UnknownBasket(val id: String) : TaskInputError
    data class UnknownItem(val id: String) : TaskInputError

    /** [remaining] > 0 — разложено не всё, < 0 — разложено больше суммы. */
    data class NotFullyDistributed(val remaining: Int) : TaskInputError

    /** Оплата сверх лимита — «не хватает [shortage] монет», как в магазине. */
    data class OverLimit(val total: Int, val limit: Int) : TaskInputError {
        val shortage: Int get() = total - limit
    }

    data class DepositOutOfRange(val max: Int) : TaskInputError
    data class DepositNotOnStep(val step: Int) : TaskInputError

    /** sorter: разложены не все вещи. */
    data class NotAllSorted(val missing: Int) : TaskInputError

    /** goal_race и change: дней или раундов не столько, сколько в задании. */
    data class WrongCount(val expected: Int) : TaskInputError

    /** reserve: нужное нельзя убрать из плана. */
    data class NeedsNotPlanned(val id: String) : TaskInputError

    /** reserve: нужное нельзя перенести ради непредвиденной траты. */
    data class NeedsLocked(val id: String) : TaskInputError

    /** reserve: перенести можно только то, что было в плане. */
    data class NotPlanned(val id: String) : TaskInputError

    /** reserve: непредвиденная трата всё ещё не покрыта — не хватает [shortage]. */
    data class SurpriseNotCovered(val shortage: Int) : TaskInputError

    /** change: такой монеты нет в ящике. */
    data class UnknownCoin(val value: Int) : TaskInputError

    /** change: в кошельке нет таких монет, раунд [round] с 0. */
    data class CoinsNotInWallet(val round: Int) : TaskInputError
}

/** Числа для объяснения исхода. */
sealed interface TaskDetails {
    /** Заполнено, если у задания есть `goal`. */
    data class Distribution(val goalSavedAfter: Int?, val goalRemaining: Int?) : TaskDetails
    data class Basket(val total: Int) : TaskDetails
    data class GoalSlider(val collected: Int, val shortfall: Int) : TaskDetails

    /** [mistakes] — id вещей, отнесённых не туда, в порядке задания. */
    data class Sorting(val correct: Int, val total: Int, val mistakes: List<String>) : TaskDetails

    /** [eventsTaken] — сколько соблазнов дня ребёнок себе позволил. */
    data class GoalRace(val saved: Int, val shortfall: Int, val eventsTaken: Int) : TaskDetails

    /** [reserve] — запас после плана; [shortage] — сколько не хватило запаса на непредвиденное. */
    data class Reserve(val reserve: Int, val shortage: Int) : TaskDetails

    /**
     * Итог дня лавки. [leftover] — непроданные стаканы, [missed] — гости, которым не хватило.
     * [best] — сколько сырья было бы в самый раз.
     */
    data class Stand(
        val cups: Int,
        val sold: Int,
        val earned: Int,
        val spent: Int,
        val leftover: Int,
        val missed: Int,
        val best: Int,
    ) : TaskDetails {
        val kept: Int get() = earned - spent
    }

    /** [results] — по раунду: положено минус нужно. 0 — верно, > 0 — лишнее, < 0 — не хватает. */
    data class Change(val correct: Int, val total: Int, val results: List<Int>) : TaskDetails
}

sealed interface TaskEvaluation {
    data class Done(val outcome: TaskOutcome, val details: TaskDetails) : TaskEvaluation
    data class Invalid(val error: TaskInputError) : TaskEvaluation
}

/** Движки заданий: три учебных (docs/economy.md, раздел 9) и пять мини-игр (docs/minigames.md). */
object TaskEngines {

    fun evaluate(task: TaskDefinition, input: TaskInput): TaskEvaluation = when {
        task is DistributorTask && input is TaskInput.Distribution -> distribute(task, input)
        task is BasketTask && input is TaskInput.Basket -> checkout(task, input)
        task is GoalSliderTask && input is TaskInput.Deposit -> slide(task, input)
        task is SorterTask && input is TaskInput.Sorting -> sort(task, input)
        task is GoalRaceTask && input is TaskInput.DailyDeposits -> race(task, input)
        task is ReserveTask && input is TaskInput.Reserve -> reserve(task, input)
        task is StandTask && input is TaskInput.Stock -> stand(task, input)
        task is ChangeTask && input is TaskInput.Coins -> change(task, input)
        else -> TaskEvaluation.Invalid(TaskInputError.WrongInputType)
    }

    // ---------- Подсказки для экранов: те же правила, что в оценке ----------

    /** Выполнено ли правило корзины для выбранных товаров — для галочек списка покупок. */
    fun ruleMet(task: BasketTask, rule: BasketRule, items: Set<String>): Boolean {
        val chosen = task.shelf.filter { it.id in items }
        return when (rule) {
            is BasketRule.HasCategory -> chosen.any { it.category == rule.category }
            is BasketRule.Includes -> rule.item in items
            is BasketRule.Excludes -> rule.item !in items
            is BasketRule.AnyOf -> rule.items.any { it in items }
            is BasketRule.MinQty -> chosen.filter { it.id in rule.items }.sumOf { it.qty } >= rule.qty
        }
    }

    /** Разница «положено минус нужно» для раунда кассы: 0 — верно. */
    fun changeDiff(round: ChangeRound, coins: List<Int>): Int = coins.sum() - round.target

    /** Можно ли заплатить этими монетами из кошелька раунда: каждой монеты — не больше, чем есть. */
    fun fitsWallet(round: ChangeRound, coins: List<Int>): Boolean {
        val have = round.wallet.groupingBy { it }.eachCount()
        return coins.groupingBy { it }.eachCount().all { (coin, n) -> n <= (have[coin] ?: 0) }
    }

    /** Итог лавки для [count] штук сырья — экран вечера показывает те же числа, что уйдут в оценку. */
    fun standDay(task: StandTask, count: Int): TaskDetails.Stand {
        val cups = count * task.ingredient.yields
        val sold = minOf(cups, task.guests)
        return TaskDetails.Stand(
            cups = cups,
            sold = sold,
            earned = sold * task.cupPrice,
            spent = count * task.ingredient.price,
            leftover = cups - sold,
            missed = task.guests - sold,
            best = bestStock(task),
        )
    }

    /** Сколько сырья хватит на всех гостей без лишнего: ⌈гости / стаканов из штуки⌉. */
    fun bestStock(task: StandTask): Int = (task.guests + task.ingredient.yields - 1) / task.ingredient.yields

    /** Запас после плана: сумма минус всё запланированное. */
    fun reserveLeft(task: ReserveTask, planned: Set<String>): Int =
        task.amount - task.spendings.filter { it.id in planned }.sumOf { it.price }

    private fun distribute(task: DistributorTask, input: TaskInput.Distribution): TaskEvaluation {
        val basketIds = task.baskets.map { it.id }.toSet()
        input.amounts.keys.firstOrNull { it !in basketIds }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownBasket(it))
        }
        if (input.amounts.values.any { it < 0 }) return TaskEvaluation.Invalid(TaskInputError.NegativeAmount)
        val remaining = task.amount - input.amounts.values.sum()
        if (remaining != 0) return TaskEvaluation.Invalid(TaskInputError.NotFullyDistributed(remaining))

        fun amountIn(basket: String) = input.amounts[basket] ?: 0
        val success = task.rules.all { rule ->
            when (rule) {
                is DistributorRule.Min -> amountIn(rule.basket) >= rule.value
                is DistributorRule.Max -> amountIn(rule.basket) <= rule.value
            }
        }
        val savedAfter = task.goal?.let { it.saved + amountIn(it.basket) }
        val details = TaskDetails.Distribution(
            goalSavedAfter = savedAfter,
            goalRemaining = task.goal?.let { maxOf(0, it.price - savedAfter!!) },
        )
        return TaskEvaluation.Done(outcome(success), details)
    }

    private fun checkout(task: BasketTask, input: TaskInput.Basket): TaskEvaluation {
        val shelf = task.shelf.associateBy { it.id }
        input.items.firstOrNull { it !in shelf }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        val chosen = input.items.map { shelf.getValue(it) }
        val total = chosen.sumOf { it.price }
        if (total > task.limit) return TaskEvaluation.Invalid(TaskInputError.OverLimit(total, task.limit))

        val success = task.rules.all { ruleMet(task, it, input.items) }
        return TaskEvaluation.Done(outcome(success), TaskDetails.Basket(total))
    }

    private fun slide(task: GoalSliderTask, input: TaskInput.Deposit): TaskEvaluation {
        if (input.amount !in 0..task.incomePerPeriod) {
            return TaskEvaluation.Invalid(TaskInputError.DepositOutOfRange(task.incomePerPeriod))
        }
        if (input.amount % task.step != 0) return TaskEvaluation.Invalid(TaskInputError.DepositNotOnStep(task.step))
        val collected = input.amount * task.periods
        val details = TaskDetails.GoalSlider(collected = collected, shortfall = maxOf(0, task.goalPrice - collected))
        return TaskEvaluation.Done(outcome(collected >= task.goalPrice), details)
    }

    private fun sort(task: SorterTask, input: TaskInput.Sorting): TaskEvaluation {
        val ids = task.items.map { it.id }.toSet()
        input.answers.keys.firstOrNull { it !in ids }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        val missing = ids.count { it !in input.answers }
        if (missing > 0) return TaskEvaluation.Invalid(TaskInputError.NotAllSorted(missing))
        val mistakes = task.items.filter { input.answers[it.id] != it.category }.map { it.id }
        val correct = task.items.size - mistakes.size
        return TaskEvaluation.Done(
            outcome(correct >= task.minCorrect),
            TaskDetails.Sorting(correct = correct, total = task.items.size, mistakes = mistakes),
        )
    }

    private fun race(task: GoalRaceTask, input: TaskInput.DailyDeposits): TaskEvaluation {
        if (input.amounts.size != task.days) return TaskEvaluation.Invalid(TaskInputError.WrongCount(task.days))
        if (input.amounts.any { it !in 0..task.incomePerDay }) {
            return TaskEvaluation.Invalid(TaskInputError.DepositOutOfRange(task.incomePerDay))
        }
        if (input.amounts.any { it % task.step != 0 }) return TaskEvaluation.Invalid(TaskInputError.DepositNotOnStep(task.step))
        val saved = task.startSaved + input.amounts.sum()
        val taken = task.events.count { event ->
            val deposit = input.amounts.getOrNull(event.day - 1) ?: return@count false
            task.incomePerDay - deposit >= event.price
        }
        return TaskEvaluation.Done(
            outcome(saved >= task.goal.price),
            TaskDetails.GoalRace(saved = saved, shortfall = maxOf(0, task.goal.price - saved), eventsTaken = taken),
        )
    }

    private fun reserve(task: ReserveTask, input: TaskInput.Reserve): TaskEvaluation {
        val byId = task.spendings.associateBy { it.id }
        (input.planned + input.dropped).firstOrNull { it !in byId }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        task.spendings.firstOrNull { it.category == Category.NEEDS && it.id !in input.planned }?.let {
            return TaskEvaluation.Invalid(TaskInputError.NeedsNotPlanned(it.id))
        }
        val reserve = reserveLeft(task, input.planned)
        if (reserve < 0) return TaskEvaluation.Invalid(TaskInputError.OverLimit(task.amount - reserve, task.amount))
        input.dropped.firstOrNull { it !in input.planned }?.let {
            return TaskEvaluation.Invalid(TaskInputError.NotPlanned(it))
        }
        input.dropped.firstOrNull { byId.getValue(it).category == Category.NEEDS }?.let {
            return TaskEvaluation.Invalid(TaskInputError.NeedsLocked(it))
        }

        val shortage = maxOf(0, task.surprise.price - reserve)
        val freed = input.dropped.sumOf { byId.getValue(it).price }
        if (freed < shortage) return TaskEvaluation.Invalid(TaskInputError.SurpriseNotCovered(shortage - freed))
        return TaskEvaluation.Done(outcome(shortage == 0), TaskDetails.Reserve(reserve = reserve, shortage = shortage))
    }

    private fun stand(task: StandTask, input: TaskInput.Stock): TaskEvaluation {
        if (input.count < 0) return TaskEvaluation.Invalid(TaskInputError.NegativeAmount)
        val cost = input.count * task.ingredient.price
        if (cost > task.budget) return TaskEvaluation.Invalid(TaskInputError.OverLimit(cost, task.budget))
        val day = standDay(task, input.count)
        return TaskEvaluation.Done(outcome(input.count == day.best), day)
    }

    private fun change(task: ChangeTask, input: TaskInput.Coins): TaskEvaluation {
        if (input.rounds.size != task.rounds.size) return TaskEvaluation.Invalid(TaskInputError.WrongCount(task.rounds.size))
        input.rounds.flatten().firstOrNull { it !in task.coins }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownCoin(it))
        }
        task.rounds.forEachIndexed { i, round ->
            if (round.mode == ChangeMode.EXACT && !fitsWallet(round, input.rounds[i])) {
                return TaskEvaluation.Invalid(TaskInputError.CoinsNotInWallet(i))
            }
        }
        val results = task.rounds.mapIndexed { i, round -> changeDiff(round, input.rounds[i]) }
        val correct = results.count { it == 0 }
        return TaskEvaluation.Done(
            outcome(correct >= (task.minCorrect ?: task.rounds.size)),
            TaskDetails.Change(correct = correct, total = task.rounds.size, results = results),
        )
    }

    private fun outcome(success: Boolean) = if (success) TaskOutcome.SUCCESS else TaskOutcome.FAIL
}
