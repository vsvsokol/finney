package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Rejection

/**
 * Весь план — намерение. Копилку ребёнок пополняет сам кнопкой «Положить», и условие
 * «копилка» засчитывается по тому, сколько прибавилось за уровень: положил и забрал — 0.
 */
class PlanSavingsTest {

    private val game = Fixtures.game()

    @Test
    fun `подтверждение плана ничего не списывает — копилка тоже обещание`() {
        val start = game.selectGoal(game.newGame(), Fixtures.goals.first().id).state()
        val budget = start.balance

        val active = game.confirmPlan(start, needs = 20, wants = 10, savings = 10).state()

        assertEquals(budget, active.currentPeriod.plan!!.budget)
        assertEquals("деньги на месте, пока не положил сам", budget, active.balance)
        assertEquals(0, active.totalSavings)
        assertEquals(10, active.currentPeriod.plan!!.savings)
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
    fun `отложенное кнопкой попадает в факт и засчитывается при закрытии`() {
        val start = game.selectGoal(game.newGame(), Fixtures.goals.first().id).state()
        val active = game.confirmPlan(start, needs = 0, wants = 0, savings = 10).state()

        val saved = game.deposit(active, 10).state()
        val result = game.closePeriod(saved).state().periods.first { it.number == 1 }.result!!

        assertEquals(10, result.facts.savings)
        assertTrue("за пополнение копилки даются очки", result.savingsAdded)
    }

    @Test
    fun `копилка в плане без «Положить» — условие не выполнено`() {
        val start = game.selectGoal(game.newGame(), Fixtures.goals.first().id).state()
        val active = game.confirmPlan(start, needs = 0, wants = 0, savings = 10).state()

        assertFalse(game.levelCheck(active).savingsAdded)
        val result = game.closePeriod(active).state().periods.first { it.number == 1 }.result!!
        assertEquals(0, result.facts.savings)
        assertFalse(result.savingsAdded)
    }

    @Test
    fun `положил и сразу забрал — за уровень ничего не прибавилось, условие не выполнено`() {
        val start = game.selectGoal(game.newGame(), Fixtures.goals.first().id).state()
        val active = game.confirmPlan(start, needs = 0, wants = 0, savings = 10).state()

        val back = game.withdraw(game.deposit(active, 10).state(), 10).state()

        assertFalse(game.levelCheck(back).savingsAdded)
        assertFalse(game.closePeriod(back).state().periods.first { it.number == 1 }.result!!.savingsAdded)
    }
}
