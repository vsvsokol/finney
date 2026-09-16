package ru.finney.pet.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.TaskResult
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.pet.PetRules
import ru.finney.pet.domain.tasks.TaskInput
import kotlin.random.Random

/**
 * 6. Случайная последовательность команд: ни одна не уводит баланс или копилку в минус,
 * шкалы остаются в 0..100, очки и уровень не убывают.
 */
class InvariantsTest {

    @Test
    fun `6 случайные команды не нарушают инварианты`() {
        var executed = 0
        repeat(20) { seed -> executed += run(Random(seed)) }
        // Защита от бесполезного теста: большая часть команд должна реально выполняться.
        assertTrue("выполнено команд: $executed из 8000", executed > 3000)
    }

    /** @return сколько команд выполнено без отказа. */
    private fun run(random: Random): Int {
        val game = Fixtures.game()
        var s = game.newGame(isDemo = random.nextBoolean())
        var points = 0
        var level = 1
        var executed = 0

        repeat(400) {
            val next: GameState? = when (random.nextInt(9)) {
                // обычно в пределах бюджета, иногда сверх — чтобы проверялись оба пути
                0 -> {
                    val third = s.balance / 3 + random.nextInt(0, 10)
                    game.confirmPlan(s, random.nextInt(-2, third + 1), random.nextInt(0, third + 1), random.nextInt(0, third + 1)).okState()
                }
                1 -> game.buy(s, Fixtures.shop.random(random).id).okState()
                2 -> game.selectGoal(s, Fixtures.goals.random(random).id).okState()
                3 -> game.deposit(s, random.nextInt(-5, 60)).okState()
                4 -> game.withdraw(s, random.nextInt(-5, 60)).okState()
                5 -> game.completeGoal(s).okState()
                6 -> game.addParentBonus(s, random.nextInt(0, 6) * 5).okState()
                7 -> (game.submitTask(s, Fixtures.tasks.random(random).id, TaskInput.Deposit(random.nextInt(0, 12) * 5))
                    as? TaskResult.Submitted)?.state
                else -> game.closePeriod(s).okState()
            }
            if (next != null) {
                s = next
                executed++
            }

            assertTrue("баланс ${s.balance}", s.balance >= 0)
            Fixtures.goals.forEach { assertTrue("копилка ${it.id}", s.goalSaved(it.id) >= 0) }
            listOf(s.pet.satiety, s.pet.hygiene, s.pet.mood).forEach {
                assertTrue("шкала $it", it in PetRules.STAT_MIN..PetRules.STAT_MAX)
            }
            assertTrue(s.points >= points)
            assertTrue(game.level(s) >= level)
            points = s.points
            level = game.level(s)
        }
        return executed
    }

    private fun GameResult.okState(): GameState? = (this as? GameResult.Ok)?.state
}
