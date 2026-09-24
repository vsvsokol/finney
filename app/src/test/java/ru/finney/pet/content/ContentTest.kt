package ru.finney.pet.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.model.TaskTheme
import ru.finney.pet.domain.pet.PetRules
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskEvaluation
import ru.finney.pet.domain.tasks.TaskInput
import java.io.File

/** Проверка настоящего контента из assets/content. Красный тест здесь = сломанный JSON или баланс. */
class ContentTest {

    private val content = ContentParser.parse { File("src/main/assets/content", it).readText() }

    @Test
    fun `контент разбирается и проходит проверку`() {
        assertEquals(emptyList<String>(), ContentValidator.validate(content))
    }

    /** Эмодзи на разных телефонах рисуются по-разному и спорят с плоским стилем макета. */
    @Test
    fun `в контенте нет эмодзи`() {
        val emoji = Regex("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2B00}-\\x{2BFF}\\x{FE0F}]")
        File("src/main/assets/content").listFiles { f -> f.extension == "json" }!!.forEach { file ->
            val found = emoji.findAll(file.readText()).map { it.value }.toList()
            assertEquals("${file.name}: эмодзи в тексте", emptyList<String>(), found)
        }
    }

    @Test
    fun `минимальный объём контента по ТЗ п 2_6`() {
        assertTrue("покупок ≥ 8", content.shop.size >= 8)
        assertTrue("оба типа покупок", content.shop.map { it.category }.toSet() == Category.entries.toSet())
        assertTrue("целей ≥ 3", content.goals.size >= 3)
        assertTrue("заданий ≥ 6", content.tasks.size >= 6)
        assertEquals("задания по 3 темам", TaskTheme.entries.toSet(), content.tasks.map { it.theme }.toSet())
        assertTrue("стадий ≥ 3", content.economy.stageStartLevels.size >= 3)
        assertTrue("справочник терминов не пуст, ТЗ п. 2.5.11", content.glossary.isNotEmpty())
    }

    @Test
    fun `11 доход каждой стадии закрывает нужное — нет тупика и есть выбор`() {
        val economy = content.economy
        val threshold = economy.pet.needsThreshold
        for (stage in 1..economy.stageStartLevels.size) {
            val income = economy.income(stage)
            val fromZero = PetRules.needsCost(PetStats(0, 0, 0), content.shop, threshold)!!
            val afterDecay = PetRules.needsCost(
                PetRules.decay(PetStats(threshold, threshold, threshold), economy.decay(stage)),
                content.shop,
                threshold,
            )!!
            assertTrue("стадия $stage: нужное из 0/0 = $fromZero > дохода $income", fromZero <= income)
            assertTrue("стадия $stage: свободно ${income - afterDecay} < 20", income - afterDecay >= 20)
        }
    }

    /** Входы для успешного и неудачного прохождения. «Конвейер» и «Касса» — в тесте ниже, по самому контенту. */
    private val cases: Map<String, Pair<TaskInput, TaskInput>> = mapOf(
        "game_shopping" to (TaskInput.Basket(setOf("apple5", "soap", "milk")) to TaskInput.Basket(setOf("apple2", "soap"))),
        "game_race" to (TaskInput.DailyDeposits(listOf(10, 10, 5, 5, 10, 10)) to TaskInput.DailyDeposits(listOf(5, 5, 5, 5, 5, 5))),
        "game_rainy" to (TaskInput.Reserve(setOf("food", "soap", "icecream"), emptySet()) to
            TaskInput.Reserve(setOf("food", "soap", "ball", "stickers", "icecream"), setOf("ball"))),
        "game_lemonade" to (TaskInput.Stock(4) to TaskInput.Stock(6)),
    )

    @Test
    fun `14 каждое задание проходится и успешно, и неудачно`() {
        for ((id, inputs) in cases) {
            val task = content.task(id) ?: throw AssertionError("нет задания $id")
            val (success, failure) = inputs
            assertEquals("$id успех", TaskOutcome.SUCCESS, (TaskEngines.evaluate(task, success) as TaskEvaluation.Done).outcome)
            assertEquals("$id неудача", TaskOutcome.FAIL, (TaskEngines.evaluate(task, failure) as TaskEvaluation.Done).outcome)
        }
    }

    @Test
    fun `мини-игры с ответами из самого контента проходятся и успешно, и неудачно`() {
        val sorter = content.task("game_sorter") as SorterTask
        val right = sorter.items.associate { it.id to it.category }
        val wrong = right.mapValues { (_, c) -> if (c == Category.NEEDS) Category.WANTS else Category.NEEDS }
        assertEquals(TaskOutcome.SUCCESS, (TaskEngines.evaluate(sorter, TaskInput.Sorting(right)) as TaskEvaluation.Done).outcome)
        assertEquals(TaskOutcome.FAIL, (TaskEngines.evaluate(sorter, TaskInput.Sorting(wrong)) as TaskEvaluation.Done).outcome)

        val cashier = content.task("game_cashier") as ChangeTask
        val exact = cashier.rounds.map { round -> greedy(round.target, round.wallet.ifEmpty { cashier.coins }, limited = round.wallet.isNotEmpty()) }
        assertEquals(TaskOutcome.SUCCESS, (TaskEngines.evaluate(cashier, TaskInput.Coins(exact)) as TaskEvaluation.Done).outcome)
        val over = cashier.rounds.map { listOf(cashier.coins.max()) }
        assertEquals(TaskOutcome.FAIL, (TaskEngines.evaluate(cashier, TaskInput.Coins(over)) as TaskEvaluation.Done).outcome)

        assertTrue("в «Дождливом дне» есть желаемое", (content.task("game_rainy") as ReserveTask).spendings.any { it.category == Category.WANTS })
    }

    /** Жадный набор суммы: крупные монеты первыми. [limited] — каждую монету из списка можно взять один раз. */
    private fun greedy(sum: Int, coins: List<Int>, limited: Boolean): List<Int> {
        var left = sum
        val pool = coins.sortedDescending().toMutableList()
        val taken = mutableListOf<Int>()
        while (left > 0) {
            val coin = pool.first { it <= left }
            taken += coin
            left -= coin
            if (limited) pool.remove(coin)
        }
        return taken
    }
}
