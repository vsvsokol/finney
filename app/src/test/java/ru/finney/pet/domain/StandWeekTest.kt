package ru.finney.pet.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.model.StandDay
import ru.finney.pet.domain.model.StandIngredient
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskTheme
import ru.finney.pet.domain.model.Weather
import ru.finney.pet.domain.tasks.StandChoice
import ru.finney.pet.domain.tasks.TaskChecks
import ru.finney.pet.domain.tasks.TaskDetails
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskEvaluation
import ru.finney.pet.domain.tasks.TaskGenerator
import ru.finney.pet.domain.tasks.TaskInput
import ru.finney.pet.domain.tasks.TaskInputError

/** Лимонадная лавка на несколько дней: docs/minigames.md, «Лавка на несколько дней». */
class StandWeekTest {

    /** Прогноз обещал солнце два дня, а во второй день пошёл дождь. */
    private val week = StandTask(
        id = "lw", theme = TaskTheme.PLANNING, title = "", intro = "", explainOk = "", explainFail = "",
        budget = 30,
        ingredient = StandIngredient("Лимон", price = 5, yields = 2),
        cupPrice = 5,
        days = listOf(
            StandDay(Weather.SUN, Weather.SUN),
            StandDay(Weather.SUN, Weather.RAIN),
            StandDay(Weather.CLOUDS, Weather.CLOUDS),
        ),
        demand = mapOf(Weather.SUN to 10, Weather.CLOUDS to 6, Weather.RAIN to 2),
        prices = listOf(4, 5, 6),
        guestsPerCoin = 2,
        freshDays = 2,
        goalProfit = 25,
    )

    @Test
    fun `дороже стакан — меньше гостей`() {
        assertEquals(10, TaskEngines.standGuests(week, Weather.SUN, 5))
        assertEquals(8, TaskEngines.standGuests(week, Weather.SUN, 6))
        assertEquals(12, TaskEngines.standGuests(week, Weather.SUN, 4))
        assertEquals(0, TaskEngines.standGuests(week, Weather.RAIN, 7))
    }

    @Test
    fun `лимоны переходят на завтра и портятся через два дня`() {
        // Утро 1: 6 лимонов на солнце, продано 10 стаканов из 5 лимонов, 1 остался.
        // Утро 2: дождь, нужен 1 лимон — берётся вчерашний. Утро 3: лимонов нет, покупаем 3.
        val days = TaskEngines.standDays(week, listOf(StandChoice(6, 5), StandChoice(2, 5), StandChoice(3, 5)))
        val (first, second, third) = days
        assertEquals(listOf(10, 5, 10, 1), listOf(first.guests, first.used, first.sold, first.stockLeft))
        assertEquals(30 - 30 + 50, first.money)
        // Во второй день 2 гостя: выжат старый лимон, новые 2 остались на завтра.
        assertEquals(listOf(2, 1, 2, 2, 0), listOf(second.guests, second.used, second.sold, second.stockLeft, second.spoiled))
        // В третий день 6 гостей: 2 лимона со вчера и 1 из 3 новых; 2 лимона — на завтра, никто не испортился.
        assertEquals(listOf(6, 3, 6, 2, 0), listOf(third.guests, third.used, third.sold, third.stockLeft, third.spoiled))
    }

    @Test
    fun `непроданный лимон портится`() {
        // Три дня дождя. Купили 6: выжат 1, 5 остались. Во второй день выжат ещё 1,
        // а вечером 4 лимона первого дня портятся.
        val rainy = week.copy(days = List(3) { StandDay(Weather.RAIN, Weather.RAIN) })
        val days = TaskEngines.standDays(rainy, listOf(StandChoice(6, 5), StandChoice(0, 5), StandChoice(0, 5)))
        assertEquals(5, days[0].stockLeft)
        assertEquals(4, days[1].spoiled)
        assertEquals(0, days[1].stockLeft)
    }

    @Test
    fun `оценка — прибыль против порога`() {
        val good = listOf(StandChoice(4, 6), StandChoice(1, 5), StandChoice(3, 5))
        val done = TaskEngines.evaluate(week, TaskInput.StandDays(good)) as TaskEvaluation.Done
        val details = done.details as TaskDetails.StandWeek
        assertEquals(details.days.last().money - week.budget, details.profit)
        assertEquals(TaskOutcome.SUCCESS, done.outcome)

        val greedy = List(3) { StandChoice(6, 5) }
        assertEquals(TaskOutcome.FAIL, (TaskEngines.evaluate(week, TaskInput.StandDays(greedy)) as TaskEvaluation.Done).outcome)
    }

    @Test
    fun `ошибки ввода — не та цена, не хватает денег, не столько дней, одиночный ввод`() {
        fun error(vararg days: StandChoice) = (TaskEngines.evaluate(week, TaskInput.StandDays(days.toList())) as TaskEvaluation.Invalid).error
        assertEquals(TaskInputError.PriceNotOffered(9), error(StandChoice(1, 9), StandChoice(0, 5), StandChoice(0, 5)))
        assertEquals(TaskInputError.OverLimit(35, 30), error(StandChoice(7, 5), StandChoice(0, 5), StandChoice(0, 5)))
        assertEquals(TaskInputError.WrongCount(3), error(StandChoice(1, 5)))
        assertEquals(TaskInputError.WrongInputType, (TaskEngines.evaluate(week, TaskInput.Stock(3)) as TaskEvaluation.Invalid).error)
    }

    @Test
    fun `проверка — порог достижим, а скупать всё не выигрывает`() {
        assertEquals(emptyList<String>(), TaskChecks.problems(week))
        assertTrue(TaskEngines.standBestProfit(week) >= week.goalProfit)
        assertTrue(TaskChecks.problems(week.copy(goalProfit = 500)).single().contains("не набрать"))
        val sunny = week.copy(days = List(3) { StandDay(Weather.SUN, Weather.SUN) }, goalProfit = 5)
        assertTrue(TaskChecks.problems(sunny).single().contains("скупать"))
    }

    /** Тот же блок, что в docs/minigames.md: вставить в tasks.json, когда будет экран на несколько дней. */
    private val template = Json.parseToJsonElement(
        """
        {
          "id": "game_lemonade_week", "theme": "planning", "engine": "stand", "title": "Лимонадная лавка",
          "intro": "Три дня лавки. Каждое утро купи лимоны и выбери цену. Прогноз — подсказка, а не обещание!",
          "explainOk": "Прибыль {goalProfit} есть! Ты смотрел на прогноз, но не скупал лишнего.",
          "explainFail": "Прибыль меньше {goalProfit}. Лимоны портятся через два дня — покупай под прогноз, а не на все деньги.",
          "budget": 30,
          "ingredient": { "label": "Лимон", "price": 5, "yields": 2, "art": "item_food_lemon" },
          "cupPrice": 5,
          "prices": [4, 5, 6],
          "guestsPerCoin": 2,
          "demand": { "sun": 10, "clouds": 6, "rain": 2 },
          "freshDays": 2,
          "goalProfit": 25,
          "days": [
            { "oneOf": [{ "forecast": "sun", "weather": "sun" }, { "forecast": "sun", "weather": "sun" }, { "forecast": "sun", "weather": "clouds" }] },
            { "oneOf": [{ "forecast": "clouds", "weather": "clouds" }, { "forecast": "clouds", "weather": "rain" }, { "forecast": "clouds", "weather": "sun" }] },
            { "oneOf": [{ "forecast": "rain", "weather": "rain" }, { "forecast": "rain", "weather": "clouds" }, { "forecast": "sun", "weather": "sun" }] }
          ]
        }
        """,
    ).jsonObject

    @Test
    fun `шаблон из документации решаем на любом зерне и погода правда меняется`() {
        val tasks = (1L..200L).map { TaskGenerator.generate(template, it) as StandTask }
        tasks.forEach { assertEquals(emptyList<String>(), TaskChecks.problems(it)) }
        assertTrue(tasks.map { it.days }.toSet().size > 5)
        assertTrue("прогноз иногда ошибается", tasks.any { t -> t.days.any { it.forecast != it.weather } })
        assertEquals("Прибыль 25 есть! Ты смотрел на прогноз, но не скупал лишнего.", tasks.first().explainOk)
    }
}
