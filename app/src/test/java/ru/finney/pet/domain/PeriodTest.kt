package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.Plan
import ru.finney.pet.domain.period.PeriodRules
import ru.finney.pet.domain.progress.Progression

/** docs/economy.md, разделы 3 и 8. */
class PeriodTest {

    private val plan = Plan(budget = 60, needs = 30, wants = 20, savings = 10)

    private fun facts(needs: Int = 30, wants: Int = 20, savings: Int = 10, unplanned: Int = 0) =
        PeriodFacts(needs, wants, savings, unplanned)

    private fun matched(f: PeriodFacts) = PeriodRules.planMatched(plan, f, tolerance = 5)

    @Test
    fun `7 сравнение плана с фактом`() {
        assertTrue("точно по плану", matched(facts()))
        assertTrue("недотрата желаемого — не ошибка", matched(facts(wants = 0)))
        assertTrue("перерасход в пределах допуска", matched(facts(wants = 25)))
        assertTrue("перерасход за счёт заработанного после плана", matched(facts(wants = 40, unplanned = 15)))
        assertFalse("перерасход сверх заработанного и допуска", matched(facts(wants = 41, unplanned = 15)))
        assertFalse("перерасход нужного без дохода", matched(facts(needs = 40)))
        assertTrue("копилка в пределах допуска", matched(facts(savings = 5)))
        assertFalse("копилка меньше плана", matched(facts(savings = 4)))
        assertTrue("копилка больше плана", matched(facts(savings = 50)))
    }

    @Test
    fun `8 очки по таблице`() {
        val rule = Fixtures.economy.points
        assertEquals(8, Progression.periodPoints(true, true, true, successfulTasks = 5, rule))
        assertEquals(8, rule.maxPerPeriod)
        assertEquals(2, Progression.periodPoints(true, false, false, 0, rule))
        assertEquals(3, Progression.periodPoints(false, false, true, 1, rule))
        assertEquals(0, Progression.periodPoints(false, false, false, 0, rule))
    }

    @Test
    fun `9 уровень 9 — ровно за 5 периодов с максимумом очков`() {
        val config = Fixtures.economy
        assertEquals(7, Progression.level(4 * config.points.maxPerPeriod, config))
        assertEquals(9, Progression.level(5 * config.points.maxPerPeriod, config))
        assertEquals(9, Progression.level(1000, config))
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3), (1..9).map { Progression.stage(it, config) })
    }

    @Test
    fun `9 полный цикл идеальной игры — стадия 3 к 4-му периоду, уровень 9 к 5-му`() {
        val game = Fixtures.game()
        var s = game.newGame()
        val levels = mutableListOf<Int>()
        repeat(5) {
            s = game.playPerfectPeriod(s)
            levels += game.level(s)
        }

        assertEquals(listOf(2, 4, 5, 7, 9), levels)
        assertTrue(s.periods.dropLast(1).all { it.result!!.points == 8 })
        assertEquals(listOf(1, 1, 2, 2, 3, 3), s.periods.map { it.stage })
        assertEquals(PeriodPhase.PLANNING, s.currentPeriod.phase)
    }

    @Test
    fun `8 уровень не убывает, даже если после хорошей игры ничего не делать`() {
        val game = Fixtures.game()
        var s = game.playPerfectPeriod(game.playPerfectPeriod(game.newGame()))
        var level = game.level(s)
        repeat(5) {
            s = game.closePeriod(game.confirmPlan(s, 0, 0, 0).state()).state()
            assertTrue(game.level(s) >= level)
            level = game.level(s)
        }
    }

    @Test
    fun `закрытие без плана отклоняется, итоги сохраняются в закрытом периоде`() {
        val game = Fixtures.game()
        val start = game.newGame()
        assertEquals(Rejection.PlanNotConfirmed, game.closePeriod(start).reason())

        val s = game.playPerfectPeriod(start)
        val closed = s.periods.first()
        assertEquals(PeriodPhase.CLOSED, closed.phase)
        assertTrue(closed.result!!.needsCovered)
        assertTrue(closed.result.planMatched)
        assertEquals(10, closed.result.facts.savings)
        assertEquals(2, closed.result.successfulTasks)
    }
}
