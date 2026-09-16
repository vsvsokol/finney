package ru.finney.pet.domain.tasks

import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.DistributorRule
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome

sealed interface TaskInput {
    /** distributor: сумма по id корзины. Отсутствующая корзина = 0. */
    data class Distribution(val amounts: Map<String, Int>) : TaskInput

    /** basket: выбранные товары с полки. */
    data class Basket(val items: Set<String>) : TaskInput

    /** goal_slider: регулярный взнос. */
    data class Deposit(val amount: Int) : TaskInput
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
}

/** Числа для объяснения исхода. */
sealed interface TaskDetails {
    /** Заполнено, если у задания есть `goal`. */
    data class Distribution(val goalSavedAfter: Int?, val goalRemaining: Int?) : TaskDetails
    data class Basket(val total: Int) : TaskDetails
    data class GoalSlider(val collected: Int, val shortfall: Int) : TaskDetails
}

sealed interface TaskEvaluation {
    data class Done(val outcome: TaskOutcome, val details: TaskDetails) : TaskEvaluation
    data class Invalid(val error: TaskInputError) : TaskEvaluation
}

/** Три движка заданий. docs/economy.md, раздел 9. */
object TaskEngines {

    fun evaluate(task: TaskDefinition, input: TaskInput): TaskEvaluation = when {
        task is DistributorTask && input is TaskInput.Distribution -> distribute(task, input)
        task is BasketTask && input is TaskInput.Basket -> checkout(task, input)
        task is GoalSliderTask && input is TaskInput.Deposit -> slide(task, input)
        else -> TaskEvaluation.Invalid(TaskInputError.WrongInputType)
    }

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

        val success = task.rules.all { rule ->
            when (rule) {
                is BasketRule.HasCategory -> chosen.any { it.category == rule.category }
                is BasketRule.Includes -> rule.item in input.items
                is BasketRule.Excludes -> rule.item !in input.items
            }
        }
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

    private fun outcome(success: Boolean) = if (success) TaskOutcome.SUCCESS else TaskOutcome.FAIL
}
