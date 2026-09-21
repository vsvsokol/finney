package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Rejection

/**
 * Копилка в плане — не намерение, а действие: сумма уходит с баланса сразу при
 * подтверждении. Нужное и желаемое остаются намерением и тратятся покупками.
 */
class PlanSavingsTest {

    private val game = Fixtures.game()

    @Test
    fun `подтверждение плана списывает копилку с баланса`() {
        val start = game.selectGoal(game.newGame(), Fixtures.goals.first().id).state()
        val budget = start.balance

        val active = game.confirmPlan(start, needs = 20, wants = 10, savings = 10).state()

        assertEquals("бюджет плана считается до откладывания", budget, active.currentPeriod.plan!!.budget)
        assertEquals("копилка ушла с баланса", budget - 10, active.balance)
        assertEquals(10, active.totalSavings)
        assertEquals(10, active.goalSaved(Fixtures.goals.first().id))
    }

    @Test
    fun `нужное и желаемое остаются на балансе до покупок`() {
        val start = game.newGame()
        val budget = start.balance

        val active = game.confirmPlan(start, needs = 20, wants = 10, savings = 0).state()

        assertEquals(budget, active.balance)
        assertEquals(0, active.totalSavings)
    }

    @Test
    fun `копилка в плане без выбранной цели не принимается, состояние не меняется`() {
        val start = game.newGame()

        val result = game.confirmPlan(start, needs = 0, wants = 0, savings = 10)

        assertEquals(Rejection.NoActiveGoal, result.reason())
        assertTrue("план не должен сохраниться", start.currentPeriod.plan == null)
    }

    @Test
    fun `отложенное за период попадает в факт и засчитывается при закрытии`() {
        val start = game.selectGoal(game.newGame(), Fixtures.goals.first().id).state()

        val active = game.confirmPlan(start, needs = 0, wants = 0, savings = 10).state()
        val closed = game.closePeriod(active).state()
        val result = closed.periods.first { it.number == 1 }.result!!

        assertEquals(10, result.facts.savings)
        assertTrue("за пополнение копилки даются очки", result.savingsAdded)
    }
}
