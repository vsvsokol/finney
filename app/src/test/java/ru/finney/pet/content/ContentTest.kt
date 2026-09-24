package ru.finney.pet.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ChangeMode
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.model.TaskDefinition
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
        // Задание ТЗ — игра; её варианты по уровням — сложность того же задания.
        assertTrue("заданий ≥ 6", content.taskSeries.size >= 6)
        assertEquals("задания по 3 темам", TaskTheme.entries.toSet(), content.taskSeries.map { it.first().theme }.toSet())
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

    @Test
    fun `каждый вариант каждой игры проходится и успешно, и неудачно`() {
        for (task in content.tasks) {
            val (success, failure) = solve(task)
            assertEquals("${task.id} успех", TaskOutcome.SUCCESS, outcome(task, success))
            assertEquals("${task.id} неудача", TaskOutcome.FAIL, outcome(task, failure))
        }
    }

    @Test
    fun `сложность растёт с уровнем — на каждом уровне с 2 по 9 что-то новое`() {
        assertTrue("у каждой игры есть вариант первого уровня", content.taskSeries.all { it.first().unlockLevel == 1 })
        assertTrue("у каждой игры несколько вариантов", content.taskSeries.all { it.size >= 2 })
        val levels = content.tasks.map { it.unlockLevel }.toSet()
        for (level in 2..content.economy.maxLevel) assertTrue("на уровне $level ничего не меняется", level in levels)
    }

    private fun outcome(task: TaskDefinition, input: TaskInput): TaskOutcome {
        val evaluation = TaskEngines.evaluate(task, input)
        return (evaluation as? TaskEvaluation.Done)?.outcome ?: throw AssertionError("${task.id}: ввод не принят — $evaluation")
    }

    /** Верный и неверный ввод, собранные по числам самого задания. */
    private fun solve(task: TaskDefinition): Pair<TaskInput, TaskInput> = when (task) {
        is SorterTask -> {
            val right = task.items.associate { it.id to it.category }
            TaskInput.Sorting(right) to TaskInput.Sorting(right.mapValues { (_, c) -> if (c == Category.NEEDS) Category.WANTS else Category.NEEDS })
        }
        is BasketTask -> {
            val win = (0 until (1 shl task.shelf.size)).asSequence()
                .map { mask -> task.shelf.filterIndexed { i, _ -> mask and (1 shl i) != 0 } }
                .first { items -> items.sumOf { it.price } <= task.limit && task.rules.all { TaskEngines.ruleMet(task, it, items.map { i -> i.id }.toSet()) } }
            TaskInput.Basket(win.map { it.id }.toSet()) to TaskInput.Basket(emptySet())
        }
        is GoalRaceTask -> TaskInput.DailyDeposits(List(task.days) { task.incomePerDay }) to TaskInput.DailyDeposits(List(task.days) { 0 })
        is ReserveTask -> {
            val needs = task.spendings.filter { it.category == Category.NEEDS }.map { it.id }.toSet()
            // Неудача: все желаемые, какие влезают, без запаса; ради сюрприза переносим самые дорогие.
            val planned = task.spendings.filter { it.category == Category.WANTS }.sortedBy { it.price }
                .fold(needs) { acc, s -> if (TaskEngines.reserveLeft(task, acc + s.id) >= 0) acc + s.id else acc }
            val shortage = task.surprise.price - TaskEngines.reserveLeft(task, planned)
            val dropped = task.spendings.filter { it.id in planned && it.category == Category.WANTS }.sortedByDescending { it.price }
                .fold(emptyList<String>()) { acc, s -> if (acc.sumOf { id -> task.spendings.first { it.id == id }.price } >= shortage) acc else acc + s.id }
            TaskInput.Reserve(needs, emptySet()) to TaskInput.Reserve(planned, dropped.toSet())
        }
        is StandTask -> {
            val best = TaskEngines.bestStock(task)
            TaskInput.Stock(best) to TaskInput.Stock(if ((best + 1) * task.ingredient.price <= task.budget) best + 1 else best - 1)
        }
        is ChangeTask -> {
            val right = task.rounds.map { round ->
                // Сдачу дают из ящика, где монет сколько угодно: хватит target / номинал каждой.
                val pool = if (round.mode == ChangeMode.EXACT) round.wallet else task.coins.flatMap { c -> List(round.target / c) { c } }
                payExact(round.target, pool)
            }
            val wrong = task.rounds.map { round ->
                val pool = if (round.mode == ChangeMode.EXACT) round.wallet else task.coins
                listOf(pool.firstOrNull { it != round.target } ?: pool.first())
            }
            TaskInput.Coins(right) to TaskInput.Coins(wrong)
        }
        else -> throw AssertionError("${task.id}: движок без решателя в тесте")
    }

    /** Точный набор [sum] из [coins], каждая монета — не больше одного раза. Перебор, а не жадность: 6 из [5, 2, 2, 2]. */
    private fun payExact(sum: Int, coins: List<Int>): List<Int> {
        val from = arrayOfNulls<List<Int>>(sum + 1).also { it[0] = emptyList() }
        for (coin in coins) for (s in sum downTo coin) if (from[s] == null && from[s - coin] != null) from[s] = from[s - coin]!! + coin
        return from[sum] ?: throw AssertionError("$sum не набрать из $coins")
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
