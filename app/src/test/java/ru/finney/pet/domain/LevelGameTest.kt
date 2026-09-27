package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Game
import kotlin.random.Random

/** Игра уровня: одна случайная мини-игра на период, обязательна для прохождения. */
class LevelGameTest {

    @Test
    fun `у нового периода есть игра уровня из открытых`() {
        val game = Fixtures.game()
        val s = game.newGame()
        val taskId = assertNotNullAndGet(s.currentPeriod.levelTaskId)
        assertTrue(game.isTaskAvailable(s, Fixtures.content.task(taskId)!!))
        // t10 открывается только с 3-го периода.
        assertNotEquals("t10", taskId)
    }

    @Test
    fun `одна и та же игра два уровня подряд не выпадает`() {
        repeat(20) { seed ->
            val game = Game(Fixtures.content, Random(seed)) { 0L }
            var s = game.newGame()
            val picked = mutableListOf(s.currentPeriod.levelTaskId!!)
            repeat(10) {
                s = game.closePeriod(game.confirmPlan(s, 0, 0, 0).state()).state()
                picked += s.currentPeriod.levelTaskId!!
            }
            picked.zipWithNext().forEach { (a, b) -> assertNotEquals("зерно $seed: $picked", a, b) }
        }
    }

    @Test
    fun `пока не выпали все игры, игра уровня не повторяется`() {
        repeat(20) { seed ->
            val game = Game(Fixtures.content, Random(seed)) { 0L }
            var s = game.newGame()
            // t10 открывается с 3-го периода, так что за 10 периодов выпадают все десять.
            repeat(9) { s = game.closePeriod(game.confirmPlan(s, 0, 0, 0).state()).state() }
            val picked = s.periods.map { it.levelTaskId }
            assertEquals("зерно $seed: $picked", Fixtures.tasks.map { it.id }.toSet(), picked.toSet())
        }
    }

    @Test
    fun `выбор случайный — разные зёрна дают разные игры`() {
        val first = (0 until 20).map { Game(Fixtures.content, Random(it)) { 0L }.newGame().currentPeriod.levelTaskId }
        assertTrue(first.toSet().size > 1)
    }

    @Test
    fun `открыта одна игра — она и повторяется`() {
        val content = Fixtures.content.copy(tasks = Fixtures.tasks.take(1))
        val game = Game(content, Random(1)) { 0L }
        val s = game.closePeriod(game.confirmPlan(game.newGame(), 0, 0, 0).state()).state()
        assertEquals(listOf("t1", "t1"), s.periods.map { it.levelTaskId })
    }

    @Test
    fun `без игры уровня уровень не пройден, даже если выполнено всё остальное`() {
        val game = Fixtures.game()
        var s = game.selectGoal(game.newGame(), "bike").state()
        s = game.confirmPlan(s, needs = game.needsHint(s)!!, wants = 0, savings = 10).state()
        s = game.coverNeeds(s)

        var check = game.levelCheck(s)
        assertEquals(3, check.met)
        assertFalse(check.gamePassed)
        assertFalse(check.willPass)

        // Неудача не засчитывается, успех — да.
        val taskId = s.currentPeriod.levelTaskId!!
        s = game.submitTask(s, taskId, Fixtures.failure).submitted().state
        assertFalse(game.levelCheck(s).gamePassed)
        s = game.submitTask(s, taskId, Fixtures.success).submitted().state
        check = game.levelCheck(s)
        assertTrue(check.gamePassed)
        assertTrue(check.willPass)

        val closed = game.closePeriod(s).state()
        assertEquals(true, closed.periods.first().result!!.gamePassed)
        assertEquals(2, game.level(closed))
    }

    @Test
    fun `другая мини-игра вместо игры уровня не считается`() {
        val game = Fixtures.game()
        var s = game.newPlannedGame()
        val other = Fixtures.tasks.first { it.id != s.currentPeriod.levelTaskId && game.isTaskAvailable(s, it) }
        s = game.submitTask(s, other.id, Fixtures.success).submitted().state
        assertFalse(game.levelCheck(s).gamePassed)

        val closed = game.closePeriod(s).state()
        assertEquals(false, closed.periods.first().result!!.gamePassed)
        assertEquals(1, game.level(closed))
    }

    @Test
    fun `игра, пройденная в прошлом периоде, в новом проходится заново`() {
        val game = Game(Fixtures.content.copy(tasks = Fixtures.tasks.take(1)), Random(1)) { 0L }
        var s = game.passLevelGame(game.newPlannedGame())
        s = game.closePeriod(s).state()
        s = game.confirmPlan(s, 0, 0, 0).state()
        assertEquals("t1", s.currentPeriod.levelTaskId)
        assertFalse(game.levelCheck(s).gamePassed)
    }

    @Test
    fun `старое сохранение без игры уровня — выдаётся при входе, закрытые периоды не трогаются`() {
        val game = Fixtures.game()
        val legacy = game.newGame().let { it.copy(periods = it.periods.map { p -> p.copy(levelTaskId = null) }) }
        assertTrue("без игры условие не действует", game.levelCheck(legacy).gamePassed)

        val assigned = game.assignLevelGame(legacy).state()
        assertNotNull(assigned.currentPeriod.levelTaskId)
        assertFalse(game.levelCheck(assigned).gamePassed)

        // Уже выдана — не меняется.
        assertEquals(assigned, game.assignLevelGame(assigned).state())
    }

    @Test
    fun `нет открытых игр — игры уровня нет и условие не действует`() {
        val game = Game(Fixtures.content.copy(tasks = emptyList()), Random(1)) { 0L }
        val s = game.newPlannedGame()
        assertNull(s.currentPeriod.levelTaskId)
        assertTrue(game.levelCheck(s).gamePassed)
    }

    private fun assertNotNullAndGet(value: String?): String {
        assertNotNull(value)
        return value!!
    }
}
