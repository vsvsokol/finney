package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ChangeMode
import ru.finney.pet.domain.model.ChangeRound
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.RaceEvent
import ru.finney.pet.domain.model.RaceGoal
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.ShelfItem
import ru.finney.pet.domain.model.SortItem
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.Spending
import ru.finney.pet.domain.model.StandIngredient
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.model.Surprise
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskTheme
import ru.finney.pet.domain.tasks.TaskDetails
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskEvaluation
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError

/** Движки мини-игр, docs/minigames.md. Настоящий контент проверяет ContentTest. */
class MiniGamesTest {

    private fun TaskEvaluation.outcome() = (this as TaskEvaluation.Done).outcome
    private fun TaskEvaluation.error() = (this as TaskEvaluation.Invalid).error

    // ---------- sorter ----------

    private val sorter = SorterTask(
        id = "s", theme = TaskTheme.PLANNING, title = "", intro = "", explainOk = "", explainFail = "",
        items = listOf(
            SortItem("apple", "", Category.NEEDS, "еда"),
            SortItem("candy", "", Category.WANTS, "сладость"),
            SortItem("soap", "", Category.NEEDS, "уход"),
        ),
        minCorrect = 2,
    )

    @Test
    fun `sorter — считает верные ответы и перечисляет ошибки`() {
        val one = TaskEngines.evaluate(
            sorter,
            TaskInput.Sorting(mapOf("apple" to Category.NEEDS, "candy" to Category.NEEDS, "soap" to Category.NEEDS)),
        )
        assertEquals(TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.Sorting(2, 3, listOf("candy"))), one)

        val two = TaskEngines.evaluate(
            sorter,
            TaskInput.Sorting(mapOf("apple" to Category.WANTS, "candy" to Category.NEEDS, "soap" to Category.NEEDS)),
        )
        assertEquals(TaskOutcome.FAIL, two.outcome())
    }

    @Test
    fun `sorter — разложено не всё или чужая вещь — ошибка ввода`() {
        assertEquals(
            TaskInputError.NotAllSorted(2),
            TaskEngines.evaluate(sorter, TaskInput.Sorting(mapOf("apple" to Category.NEEDS))).error(),
        )
        assertEquals(
            TaskInputError.UnknownItem("robot"),
            TaskEngines.evaluate(sorter, TaskInput.Sorting(mapOf("robot" to Category.WANTS))).error(),
        )
    }

    // ---------- basket: новые правила списка ----------

    private val shopping = BasketTask(
        id = "b", theme = TaskTheme.SHOPPING, title = "", intro = "", explainOk = "", explainFail = "",
        limit = 50,
        shelf = listOf(
            ShelfItem("apple2", "", 10, Category.NEEDS, qty = 2),
            ShelfItem("apple5", "", 20, Category.NEEDS, qty = 5),
            ShelfItem("soap", "", 10, Category.NEEDS),
            ShelfItem("soap_liquid", "", 15, Category.NEEDS),
            ShelfItem("chocolate", "", 15, Category.WANTS),
        ),
        rules = listOf(
            BasketRule.MinQty(listOf("apple2", "apple5"), qty = 4, label = "4 яблока"),
            BasketRule.AnyOf(listOf("soap", "soap_liquid"), label = "Мыло"),
        ),
    )

    @Test
    fun `basket — minQty считает штуки в упаковках, anyOf — любой из товаров`() {
        fun evaluate(vararg items: String) = TaskEngines.evaluate(shopping, TaskInput.Basket(items.toSet()))

        assertEquals(TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.Basket(45)), evaluate("apple5", "soap_liquid", "soap"))
        assertEquals(TaskOutcome.FAIL, evaluate("apple2", "soap").outcome())
        assertEquals(TaskOutcome.FAIL, evaluate("apple5", "chocolate").outcome())
        assertEquals(10, (evaluate("apple5", "soap_liquid", "chocolate", "apple2").error() as TaskInputError.OverLimit).shortage)
    }

    @Test
    fun `ruleMet — галочки списка совпадают с оценкой`() {
        val (apples, soap) = shopping.rules
        assertFalse(TaskEngines.ruleMet(shopping, apples, setOf("apple2")))
        assertTrue(TaskEngines.ruleMet(shopping, apples, setOf("apple2", "apple5")))
        assertTrue(TaskEngines.ruleMet(shopping, soap, setOf("soap_liquid")))
        assertFalse(TaskEngines.ruleMet(shopping, soap, emptySet()))
    }

    // ---------- goal_race ----------

    private val race = GoalRaceTask(
        id = "r", theme = TaskTheme.SAVINGS, title = "", intro = "", explainOk = "", explainFail = "",
        goal = RaceGoal("Самокат", 50),
        days = 6,
        incomePerDay = 10,
        events = listOf(RaceEvent(4, "Мороженое", 5), RaceEvent(5, "Кино", 10)),
    )

    @Test
    fun `goal_race — сумма взносов против цены, соблазны считаются по потраченному`() {
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.GoalRace(saved = 50, shortfall = 0, eventsTaken = 1, mood = 2)),
            TaskEngines.evaluate(race, TaskInput.DailyDeposits(listOf(10, 10, 10, 5, 5, 10))),
        )
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.FAIL, TaskDetails.GoalRace(saved = 40, shortfall = 10, eventsTaken = 2, mood = 3)),
            TaskEngines.evaluate(race, TaskInput.DailyDeposits(listOf(10, 10, 10, 5, 0, 5))),
        )
    }

    /** Как «Самокат» в контенте: свободных монет 10, соблазнов три, сердечек три. */
    private val moodRace = race.copy(
        events = listOf(RaceEvent(2, "Наклейки", 5), RaceEvent(4, "Мороженое", 5), RaceEvent(5, "Кино", 10)),
    )

    private fun raceOf(task: GoalRaceTask, vararg deposits: Int) =
        (TaskEngines.evaluate(task, TaskInput.DailyDeposits(deposits.toList())) as TaskEvaluation.Done)

    @Test
    fun `goal_race — откладывать всё больше не лучшая стратегия`() {
        val saveAll = raceOf(moodRace, 10, 10, 10, 10, 10, 10)
        // Копилка полнее всех, но Финни ни разу себя не порадовал — и цель не засчитана.
        assertEquals(TaskOutcome.FAIL, saveAll.outcome)
        assertEquals(TaskDetails.GoalRace(saved = 60, shortfall = 0, eventsTaken = 0, mood = 0), saveAll.details)

        // Одна маленькая радость — и цель, и настроение.
        val balanced = raceOf(moodRace, 10, 5, 10, 10, 10, 10)
        assertEquals(TaskOutcome.SUCCESS, balanced.outcome)
        assertEquals(1, (balanced.details as TaskDetails.GoalRace).mood)

        // Всё на радости — Финни счастлив, но цель не набрана.
        assertEquals(TaskOutcome.FAIL, raceOf(moodRace, 10, 5, 10, 5, 0, 10).outcome)
    }

    @Test
    fun `goal_race — сердечко гаснет за отказ, загорается за радость, не выше старта`() {
        // Наклейки взял (3, выше не бывает), от мороженого отказался (2), кино взял (3).
        assertEquals(listOf(3, 3, 3, 2, 3, 3), TaskEngines.raceMood(moodRace, listOf(10, 5, 10, 10, 0, 10)))
    }

    @Test
    fun `goal_race — погасшие сердечки поздняя радость не возвращает`() {
        val lateOnly = race.copy(
            mood = 2,
            events = listOf(RaceEvent(1, "Наклейки", 5), RaceEvent(2, "Мороженое", 5), RaceEvent(6, "Кино", 5)),
        )
        // Два отказа подряд — ноль. Радость в последний день уже не выручает.
        assertEquals(listOf(1, 0, 0, 0, 0, 0), TaskEngines.raceMood(lateOnly, listOf(10, 10, 10, 10, 10, 5)))
        assertEquals(TaskOutcome.FAIL, raceOf(lateOnly, 10, 10, 10, 10, 10, 5).outcome)
    }

    @Test
    fun `goal_race — решатель находит путь с радостями, если он есть`() {
        val plan = TaskEngines.raceSolution(moodRace)!!
        assertEquals(TaskOutcome.SUCCESS, raceOf(moodRace, *plan.toIntArray()).outcome)
        // Свободных монет нет вовсе: копить можно только всё, а это грусть — выиграть нельзя.
        assertEquals(null, TaskEngines.raceSolution(moodRace.copy(goal = RaceGoal("Самокат", 60))))
    }

    @Test
    fun `goal_race — не тот день или взнос — ошибка ввода`() {
        assertEquals(TaskInputError.WrongCount(6), TaskEngines.evaluate(race, TaskInput.DailyDeposits(listOf(10))).error())
        assertEquals(
            TaskInputError.DepositOutOfRange(10),
            TaskEngines.evaluate(race, TaskInput.DailyDeposits(listOf(15, 10, 10, 10, 10, 10))).error(),
        )
        assertEquals(
            TaskInputError.DepositNotOnStep(5),
            TaskEngines.evaluate(race, TaskInput.DailyDeposits(listOf(7, 10, 10, 10, 10, 10))).error(),
        )
    }

    // ---------- reserve ----------

    private val rainy = ReserveTask(
        id = "w", theme = TaskTheme.PLANNING, title = "", intro = "", explainOk = "", explainFail = "",
        amount = 60,
        spendings = listOf(
            Spending("food", "", 20, Category.NEEDS),
            Spending("soap", "", 10, Category.NEEDS),
            Spending("ball", "", 10, Category.WANTS),
            Spending("stickers", "", 10, Category.WANTS),
            Spending("icecream", "", 5, Category.WANTS),
        ),
        surprise = Surprise("Зонт", "", 15),
    )

    @Test
    fun `reserve — хватило запаса — успех, пришлось переносить — неудача`() {
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.Reserve(reserve = 20, shortage = 0)),
            TaskEngines.evaluate(rainy, TaskInput.Reserve(setOf("food", "soap", "ball"), emptySet())),
        )
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.FAIL, TaskDetails.Reserve(reserve = 5, shortage = 10)),
            TaskEngines.evaluate(
                rainy,
                TaskInput.Reserve(setOf("food", "soap", "ball", "stickers", "icecream"), setOf("ball")),
            ),
        )
    }

    @Test
    fun `reserve — нужное нельзя убрать или перенести, непредвиденное должно быть покрыто`() {
        val all = setOf("food", "soap", "ball", "stickers", "icecream")
        assertEquals(TaskInputError.NeedsNotPlanned("soap"), TaskEngines.evaluate(rainy, TaskInput.Reserve(setOf("food"), emptySet())).error())
        assertEquals(TaskInputError.NeedsLocked("food"), TaskEngines.evaluate(rainy, TaskInput.Reserve(all, setOf("food"))).error())
        assertEquals(TaskInputError.NotPlanned("ball"), TaskEngines.evaluate(rainy, TaskInput.Reserve(setOf("food", "soap"), setOf("ball"))).error())
        assertEquals(TaskInputError.SurpriseNotCovered(5), TaskEngines.evaluate(rainy, TaskInput.Reserve(all, setOf("icecream"))).error())
    }

    // ---------- stand ----------

    private val stand = StandTask(
        id = "l", theme = TaskTheme.PLANNING, title = "", intro = "", explainOk = "", explainFail = "",
        budget = 50,
        ingredient = StandIngredient("Лимон", price = 5, yields = 2),
        cupPrice = 5,
        guests = 8,
        forecast = "",
    )

    @Test
    fun `stand — успех, когда сырья ровно на всех гостей`() {
        assertEquals(4, TaskEngines.bestStock(stand))
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.Stand(cups = 8, sold = 8, earned = 40, spent = 20, leftover = 0, missed = 0, best = 4)),
            TaskEngines.evaluate(stand, TaskInput.Stock(4)),
        )
        val over = TaskEngines.standDay(stand, 5)
        assertEquals(2, over.leftover)
        assertEquals(15, over.kept)
        assertEquals(TaskOutcome.FAIL, TaskEngines.evaluate(stand, TaskInput.Stock(5)).outcome())
        assertEquals(2, TaskEngines.standDay(stand, 3).missed)
        assertEquals(TaskOutcome.FAIL, TaskEngines.evaluate(stand, TaskInput.Stock(3)).outcome())
    }

    @Test
    fun `stand — закупка дороже бюджета — отказ с нехваткой`() {
        assertEquals(5, (TaskEngines.evaluate(stand, TaskInput.Stock(11)).error() as TaskInputError.OverLimit).shortage)
        assertEquals(TaskInputError.NegativeAmount, TaskEngines.evaluate(stand, TaskInput.Stock(-1)).error())
    }

    // ---------- change ----------

    private val cashier = ChangeTask(
        id = "c", theme = TaskTheme.SHOPPING, title = "", intro = "", explainOk = "", explainFail = "",
        coins = listOf(1, 2, 5, 10),
        rounds = listOf(
            ChangeRound(ChangeMode.GIVE, "Сок", price = 17, paid = 20),
            ChangeRound(ChangeMode.EXACT, "Молоко", price = 17, wallet = listOf(10, 5, 5, 2, 1, 1)),
        ),
    )

    @Test
    fun `change — сдача и оплата без сдачи, результат по раундам`() {
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.SUCCESS, TaskDetails.Change(correct = 2, total = 2, results = listOf(0, 0))),
            TaskEngines.evaluate(cashier, TaskInput.Coins(listOf(listOf(2, 1), listOf(10, 5, 2)))),
        )
        assertEquals(
            TaskEvaluation.Done(TaskOutcome.FAIL, TaskDetails.Change(correct = 1, total = 2, results = listOf(1, 0))),
            TaskEngines.evaluate(cashier, TaskInput.Coins(listOf(listOf(2, 2), listOf(10, 5, 1, 1)))),
        )
    }

    @Test
    fun `change — чужая монета, монет нет в кошельке, не то число раундов — ошибка ввода`() {
        assertEquals(TaskInputError.UnknownCoin(3), TaskEngines.evaluate(cashier, TaskInput.Coins(listOf(listOf(3), listOf(10, 5, 2)))).error())
        assertEquals(TaskInputError.CoinsNotInWallet(1), TaskEngines.evaluate(cashier, TaskInput.Coins(listOf(listOf(2, 1), listOf(10, 2, 2, 2, 1)))).error())
        assertEquals(TaskInputError.WrongCount(2), TaskEngines.evaluate(cashier, TaskInput.Coins(listOf(listOf(2, 1)))).error())
    }

    @Test
    fun `мини-игра — обычное задание — награда и попытки общие`() {
        val game = ru.finney.pet.domain.game.Game(Fixtures.content.copy(tasks = Fixtures.tasks + stand))
        val first = game.submitTask(game.newPlannedGame(), "l", TaskInput.Stock(5)).submitted()
        assertEquals(TaskOutcome.FAIL, first.outcome)
        assertEquals(5, first.reward)
        val second = game.submitTask(first.state, "l", TaskInput.Stock(4)).submitted()
        assertEquals(15, second.reward)
        assertEquals(70, second.state.balance)
    }
}
