package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.EntryType
import ru.finney.pet.domain.model.PeriodPhase

/** docs/economy.md, разделы 1–4. Номера в названиях — инварианты из раздела 12. */
class BudgetAndPurchaseTest {

    private val game = Fixtures.game()
    private val start = game.newGame()

    @Test
    fun `новая игра — первый период в планировании, начислен доход, шкалы снижены`() {
        assertEquals(1, start.currentPeriod.number)
        assertEquals(PeriodPhase.PLANNING, start.currentPeriod.phase)
        assertEquals(50, start.balance)
        assertEquals(EntryType.INCOME, start.ledger.single().type)
        assertEquals(30, start.pet.satiety)
        assertEquals(40, start.pet.hygiene)
        assertEquals(50, start.pet.mood)
    }

    @Test
    fun `1 покупка при нехватке средств отклоняется, баланс не меняется, известна нехватка`() {
        var s = game.confirmPlan(start, needs = 50, wants = 0, savings = 0).state()
        s = game.buy(s, "lamp").state()
        s = game.buy(s, "ball").state()
        assertEquals(10, s.balance)

        val reason = game.buy(s, "bowl").reason()

        assertEquals(Rejection.InsufficientFunds(needed = 20, balance = 10), reason)
        assertEquals(10, (reason as Rejection.InsufficientFunds).shortage)
        assertEquals(10, s.balance)
    }

    @Test
    fun `2 покупка и копилка до подтверждения плана отклоняются`() {
        val withGoal = game.selectGoal(start, "bike").state()

        assertEquals(Rejection.PlanNotConfirmed, game.buy(withGoal, "apple").reason())
        assertEquals(Rejection.PlanNotConfirmed, game.deposit(withGoal, 10).reason())
        assertEquals(Rejection.PlanNotConfirmed, game.withdraw(withGoal, 10).reason())
    }

    @Test
    fun `3 план больше бюджета не подтверждается, остаток считается`() {
        assertEquals(
            Rejection.PlanExceedsBudget(budget = 50, planned = 55),
            game.confirmPlan(start, needs = 30, wants = 20, savings = 5).reason(),
        )

        val plan = game.confirmPlan(start, needs = 20, wants = 10, savings = 5).state().currentPeriod.plan!!
        assertEquals(50, plan.budget)
        assertEquals(15, plan.remainder)
    }

    @Test
    fun `план с отрицательной частью и повторное подтверждение отклоняются`() {
        assertEquals(Rejection.InvalidAmount, game.confirmPlan(start, needs = -5, wants = 0, savings = 0).reason())

        val active = game.confirmPlan(start, needs = 20, wants = 0, savings = 0).state()
        assertEquals(Rejection.PlanAlreadyConfirmed, game.confirmPlan(active, 10, 0, 0).reason())
    }

    @Test
    fun `задание до плана увеличивает бюджет и не считается незапланированным`() {
        var s = game.submitTask(start, "t1", Fixtures.success).submitted().state
        assertEquals(65, s.balance)
        assertTrue(s.ledger.none { it.unplanned })

        s = game.confirmPlan(s, needs = 20, wants = 0, savings = 0).state()
        assertEquals(65, s.currentPeriod.plan!!.budget)

        s = game.submitTask(s, "t2", Fixtures.success).submitted().state
        assertTrue(s.ledger.last().unplanned)
    }

    @Test
    fun `покупка списывает цену, пишет категорию и меняет шкалы`() {
        val active = game.confirmPlan(start, needs = 20, wants = 0, savings = 0).state()
        val preview = game.previewPurchase(active, "apple")!!

        val s = game.buy(active, "apple").state()

        assertEquals(40, s.balance)
        assertEquals(Category.NEEDS, s.ledger.last().category)
        assertEquals(50, s.pet.satiety)
        assertEquals(s.pet, preview.statsAfter)
        assertEquals(0, preview.shortage)
    }

    @Test
    fun `аксессуар покупается один раз, расходник — сколько угодно`() {
        var s = game.confirmPlan(start, needs = 0, wants = 50, savings = 0).state()
        s = game.buy(s, "hat").state()

        assertEquals(Rejection.AlreadyOwned, game.buy(s, "hat").reason())
        s = game.buy(s, "candy").state()
        s = game.buy(s, "candy").state()
        assertEquals(10, s.balance)
    }

    @Test
    fun `бонус взрослого — шаг и лимит за период`() {
        assertEquals(Rejection.BonusNotOnStep(5), game.addParentBonus(start, 7).reason())

        val s = game.addParentBonus(start, 15).state()
        assertEquals(65, s.balance)
        assertEquals(Rejection.BonusLimitExceeded(left = 5), game.addParentBonus(s, 10).reason())
    }
}
