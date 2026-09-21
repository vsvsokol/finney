package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.GameState

/** docs/economy.md, раздел 6. */
class SavingsTest {

    private val game = Fixtures.game()

    /**
     * Активный период с выбранной целью. Копилка в плане нулевая: план с копилкой
     * сразу списывает сумму с баланса, а этим тестам нужны деньги на сами пополнения.
     */
    private fun activeWithGoal(): GameState {
        val s = game.selectGoal(game.newGame(), "bike").state()
        return game.confirmPlan(s, needs = 0, wants = 0, savings = 0).state()
    }

    @Test
    fun `4 перевод в копилку уменьшает баланс и увеличивает копилку цели на одну сумму`() {
        val before = activeWithGoal()

        val after = game.deposit(before, 30).state()

        assertEquals(before.balance - 30, after.balance)
        assertEquals(before.goalSaved("bike") + 30, after.goalSaved("bike"))
        assertEquals(before.balance + before.totalSavings, after.balance + after.totalSavings)
    }

    @Test
    fun `5 снятие больше накопленного на цели отклоняется`() {
        val s = game.deposit(activeWithGoal(), 20).state()

        assertEquals(Rejection.InsufficientSavings(requested = 25, saved = 20), game.withdraw(s, 25).reason())
        assertEquals(10, game.withdraw(s, 10).state().goalSaved("bike"))
    }

    @Test
    fun `перевод больше баланса и без выбранной цели отклоняется`() {
        assertEquals(Rejection.InsufficientFunds(needed = 60, balance = 50), game.deposit(activeWithGoal(), 60).reason())

        val noGoal = game.confirmPlan(game.newGame(), 0, 0, 0).state()
        assertEquals(Rejection.NoActiveGoal, game.deposit(noGoal, 10).reason())
    }

    @Test
    fun `смена цели не сжигает накопленное`() {
        var s = game.deposit(activeWithGoal(), 30).state()
        s = game.selectGoal(s, "crown").state()
        s = game.deposit(s, 10).state()

        assertEquals(30, s.goalSaved("bike"))
        assertEquals(10, s.goalSaved("crown"))
        assertEquals(40, s.totalSavings)
    }

    @Test
    fun `13 срок не считается без пополнений, снятие срок не сокращает`() {
        var s = activeWithGoal()
        assertNull(game.goalProgress(s)!!.periodsToGoal)

        s = game.deposit(s, 20).state()
        s = game.closePeriod(s).state()
        val progress = game.goalProgress(s)!!
        assertEquals(20, progress.averageDeposit)
        assertEquals(4, progress.periodsToGoal) // осталось 80 при 20 за период

        s = game.confirmPlan(s, 0, 0, 0).state()
        val preview = game.previewWithdraw(s, 20)!!
        assertEquals(0, preview.savedAfter)
        assertEquals(4, preview.periodsBefore)
        assertEquals(5, preview.periodsAfter)
        assertTrue(preview.periodsAfter!! >= preview.periodsBefore!!)
    }

    @Test
    fun `достижение цели списывает цену из копилки, не трогает баланс и поднимает настроение`() {
        var s = game.selectGoal(game.newGame(), "crown").state()
        s = game.addParentBonus(s, 20).state()
        s = game.confirmPlan(s, 0, 0, 0).state()
        assertEquals(Rejection.GoalNotReached(price = 50, saved = 0), game.completeGoal(s).reason())

        s = game.deposit(s, 60).state()
        val before = s
        s = game.completeGoal(s).state()

        assertEquals(before.balance, s.balance)
        assertEquals(10, s.goalSaved("crown"))
        assertEquals(before.pet.mood + 20, s.pet.mood)
        assertNull(s.activeGoalId)
        assertEquals(Rejection.GoalAlreadyCompleted, game.selectGoal(s, "crown").reason())
    }
}
