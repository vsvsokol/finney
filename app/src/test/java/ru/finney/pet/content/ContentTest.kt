package ru.finney.pet.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.model.Category
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

    @Test
    fun `минимальный объём контента по ТЗ п 2_6`() {
        assertTrue("покупок ≥ 8", content.shop.size >= 8)
        assertTrue("оба типа покупок", content.shop.map { it.category }.toSet() == Category.entries.toSet())
        assertTrue("целей ≥ 3", content.goals.size >= 3)
        assertTrue("заданий ≥ 6", content.tasks.size >= 6)
        assertEquals("задания по 3 темам", TaskTheme.entries.toSet(), content.tasks.map { it.theme }.toSet())
        assertTrue("стадий ≥ 3", content.economy.stageStartLevels.size >= 3)
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

    /** Входы для успешного и неудачного прохождения. Новое задание сюда добавлять не обязательно. */
    private val cases: Map<String, Pair<TaskInput, TaskInput>> = mapOf(
        "budget_01" to (distribution("needs" to 30, "wants" to 20, "savings" to 10) to
            distribution("needs" to 10, "wants" to 45, "savings" to 5)),
        "budget_02" to (TaskInput.Basket(setOf("soup", "apple")) to TaskInput.Basket(setOf("robot", "apple"))),
        "savings_01" to (TaskInput.Deposit(30) to TaskInput.Deposit(20)),
        "savings_02" to (distribution("savings" to 40) to distribution("wants" to 40)),
        "shopping_01" to (TaskInput.Basket(setOf("pack_big")) to TaskInput.Basket(setOf("pack_small"))),
        "shopping_02" to (TaskInput.Basket(setOf("soup", "ball")) to TaskInput.Basket(setOf("candy", "ball"))),
    )

    @Test
    fun `14 каждое из 6 заданий проходится и успешно, и неудачно`() {
        for ((id, inputs) in cases) {
            val task = content.task(id) ?: throw AssertionError("нет задания $id")
            val (success, failure) = inputs
            assertEquals("$id успех", TaskOutcome.SUCCESS, (TaskEngines.evaluate(task, success) as TaskEvaluation.Done).outcome)
            assertEquals("$id неудача", TaskOutcome.FAIL, (TaskEngines.evaluate(task, failure) as TaskEvaluation.Done).outcome)
        }
    }

    private fun distribution(vararg amounts: Pair<String, Int>) = TaskInput.Distribution(amounts.toMap())
}
