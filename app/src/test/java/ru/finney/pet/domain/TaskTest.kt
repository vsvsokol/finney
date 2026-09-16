package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.TaskResult
import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.DistributorRule
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.ShelfItem
import ru.finney.pet.domain.model.TaskBasket
import ru.finney.pet.domain.model.TaskGoalPreview
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskTheme
import ru.finney.pet.domain.tasks.TaskDetails
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskEvaluation
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError

/** docs/economy.md, раздел 9. */
class TaskTest {

    private val game = Fixtures.game()

    @Test
    fun `12 награда за успех выдаётся один раз, за неудачу — только за первую попытку`() {
        var s = game.newGame()

        val firstFail = game.submitTask(s, "t1", Fixtures.failure).submitted()
        assertEquals(TaskOutcome.FAIL, firstFail.outcome)
        assertEquals(5, firstFail.reward)
        s = firstFail.state

        assertEquals(0, game.submitTask(s, "t1", Fixtures.failure).submitted().reward)

        val success = game.submitTask(s, "t1", Fixtures.success).submitted()
        assertEquals(15, success.reward)
        s = success.state

        assertEquals(0, game.submitTask(s, "t1", Fixtures.success).submitted().reward)
        assertEquals(70, s.balance)
    }

    @Test
    fun `повторный успех не даёт очков`() {
        var s = game.newGame()
        s = game.submitTask(s, "t1", Fixtures.success).submitted().state
        s = game.closePeriod(game.confirmPlan(s, 0, 0, 0).state()).state()
        s = game.submitTask(s, "t1", Fixtures.success).submitted().state
        s = game.closePeriod(game.confirmPlan(s, 0, 0, 0).state()).state()

        assertEquals(listOf(1, 0), s.periods.dropLast(1).map { it.result!!.successfulTasks })
    }

    @Test
    fun `закрытое задание недоступно, в демо-режиме открыто`() {
        assertEquals(TaskResult.Rejected(Rejection.TaskLocked), game.submitTask(game.newGame(), "t10", Fixtures.success))
        game.submitTask(game.newGame(isDemo = true), "t10", Fixtures.success).submitted()
    }

    @Test
    fun `ошибка ввода — не исход, состояние не меняется`() {
        assertEquals(
            TaskResult.Rejected(Rejection.InvalidTaskInput(TaskInputError.DepositNotOnStep(5))),
            game.submitTask(game.newGame(), "t1", TaskInput.Deposit(7)),
        )
        assertEquals(
            TaskResult.Rejected(Rejection.InvalidTaskInput(TaskInputError.WrongInputType)),
            game.submitTask(game.newGame(), "t1", TaskInput.Basket(emptySet())),
        )
    }

    private val distributor = DistributorTask(
        id = "d", theme = TaskTheme.PLANNING, title = "", intro = "", explainOk = "", explainFail = "",
        amount = 60,
        baskets = listOf(TaskBasket("needs", ""), TaskBasket("wants", ""), TaskBasket("savings", "")),
        rules = listOf(DistributorRule.Min("needs", 25), DistributorRule.Max("wants", 20)),
        goal = TaskGoalPreview("Велосипед", price = 150, saved = 100, basket = "savings"),
    )

    @Test
    fun `distributor — вся сумма разложена, правила min и max, влияние на цель`() {
        fun evaluate(vararg amounts: Pair<String, Int>) =
            TaskEngines.evaluate(distributor, TaskInput.Distribution(amounts.toMap()))

        assertEquals(
            TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.Distribution(goalSavedAfter = 115, goalRemaining = 35)),
            evaluate("needs" to 25, "wants" to 20, "savings" to 15),
        )
        assertEquals(TaskOutcome.FAIL, (evaluate("needs" to 20, "wants" to 20, "savings" to 20) as TaskEvaluation.Done).outcome)
        assertEquals(TaskOutcome.FAIL, (evaluate("needs" to 30, "wants" to 30) as TaskEvaluation.Done).outcome)
        assertEquals(TaskEvaluation.Invalid(TaskInputError.NotFullyDistributed(10)), evaluate("needs" to 50))
        assertEquals(TaskEvaluation.Invalid(TaskInputError.UnknownBasket("toys")), evaluate("toys" to 60))
        assertEquals(TaskEvaluation.Invalid(TaskInputError.NegativeAmount), evaluate("needs" to 70, "wants" to -10))
    }

    private val basket = BasketTask(
        id = "b", theme = TaskTheme.SHOPPING, title = "", intro = "", explainOk = "", explainFail = "",
        limit = 50,
        shelf = listOf(
            ShelfItem("soup", "", 25, Category.NEEDS),
            ShelfItem("candy", "", 15, Category.WANTS),
            ShelfItem("ball", "", 15, Category.WANTS),
        ),
        rules = listOf(BasketRule.HasCategory(Category.NEEDS), BasketRule.Excludes("candy")),
    )

    @Test
    fun `basket — оплата сверх лимита отклоняется с нехваткой, иначе проверяются правила`() {
        fun evaluate(vararg items: String) = TaskEngines.evaluate(basket, TaskInput.Basket(items.toSet()))

        val over = (evaluate("soup", "candy", "ball") as TaskEvaluation.Invalid).error as TaskInputError.OverLimit
        assertEquals(5, over.shortage)
        assertEquals(TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.Basket(40)), evaluate("soup", "ball"))
        assertEquals(TaskOutcome.FAIL, (evaluate("soup", "candy") as TaskEvaluation.Done).outcome)
        assertEquals(TaskOutcome.FAIL, (evaluate("ball") as TaskEvaluation.Done).outcome)
        assertEquals(TaskEvaluation.Invalid(TaskInputError.UnknownItem("robot")), evaluate("robot"))
    }

    @Test
    fun `goal_slider — взнос в пределах дохода и по шагу`() {
        val task = Fixtures.tasks.first()
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.FAIL, TaskDetails.GoalSlider(collected = 15, shortfall = 15)),
            TaskEngines.evaluate(task, TaskInput.Deposit(5)),
        )
        assertEquals(TaskOutcome.SUCCESS, (TaskEngines.evaluate(task, TaskInput.Deposit(10)) as TaskEvaluation.Done).outcome)
        assertEquals(TaskEvaluation.Invalid(TaskInputError.DepositOutOfRange(50)), TaskEngines.evaluate(task, TaskInput.Deposit(55)))
    }
}
