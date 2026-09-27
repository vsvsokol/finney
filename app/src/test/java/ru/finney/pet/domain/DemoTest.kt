package ru.finney.pet.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.content.ContentParser
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameStore
import ru.finney.pet.domain.game.ProfileResult
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.progress.Progression
import java.io.File
import kotlin.random.Random

/**
 * Демо-режим для экспертов: все временные процессы короткие, переход уровня облегчён,
 * а обычный режим остаётся прежним. Уровень, стадия и период — одно и то же: 6 уровней.
 */
class DemoTest {

    private var now = 0L
    private val game = Game(Fixtures.content, Random(7)) { now }
    private val economy = Fixtures.economy

    @Test
    fun `в демо сон, игра с игрушкой и напоминания короткие`() {
        val demo = game.newGame(isDemo = true)
        assertEquals(economy.pet.demoSleepSeconds * 1_000L, game.sleepMillis(demo))
        assertEquals(economy.play.demoSessionSeconds * 1_000L, game.playSessionMillis(demo))
        assertEquals(economy.reminders.demoReminderMinutes * 60_000L, game.reminderEveryMillis(demo))
    }

    @Test
    fun `в обычном режиме сроки прежние`() {
        val normal = game.newGame()
        assertEquals(economy.pet.sleepMinutes * 60_000L, game.sleepMillis(normal))
        assertEquals(economy.play.sessionMinutes * 60_000L, game.playSessionMillis(normal))
        assertEquals(economy.reminders.everyHours * 3_600_000L, game.reminderEveryMillis(normal))
    }

    /** План с копилкой — и сразу конец уровня: ни еды, ни игры уровня. */
    private fun planAndClose(start: GameState): GameState {
        val withGoal = if (start.activeGoalId == null) game.selectGoal(start, "bike").state() else start
        val planned = game.confirmPlan(withGoal, needs = 5, wants = 5, savings = 5).state()
        return game.closePeriod(planned).state()
    }

    @Test
    fun `в демо уровень проходится планом с копилкой, игра уровня не обязательна`() {
        val start = game.newGame(isDemo = true)
        assertTrue("игра уровня выдана и в демо", start.currentPeriod.levelTaskId != null)
        val check = game.levelCheck(start)
        assertEquals(economy.demoConditionsToPass, check.toPass)
        assertFalse(check.gameRequired)

        val next = planAndClose(start)
        assertEquals(2, game.level(next))
        assertEquals(true, next.periods.first().result!!.passed)
    }

    @Test
    fun `в обычном режиме тот же уровень не пройден`() {
        val start = game.newGame()
        val check = game.levelCheck(start)
        assertEquals(economy.conditionsToPass, check.toPass)
        assertTrue(check.gameRequired)

        val next = planAndClose(start)
        assertEquals(1, game.level(next))
        assertEquals(false, next.periods.first().result!!.passed)
    }

    @Test
    fun `выключили демо — пройденные уровни не пересчитываются`() {
        var s = game.newGame(isDemo = true)
        repeat(3) { s = planAndClose(s) }
        assertEquals(4, game.level(s))

        val normal = s.copy(isDemo = false)
        assertEquals(4, game.level(normal))
        assertEquals(economy.conditionsToPass, game.levelCheck(normal).toPass)
    }

    @Test
    fun `итоги до базы версии 8 решаются по обычным правилам`() {
        val legacy = planAndClose(game.newGame(isDemo = true)).let { s ->
            s.copy(periods = s.periods.map { p -> p.copy(result = p.result?.copy(passed = null)) })
        }
        assertEquals(false, Progression.passed(legacy.periods.first().result!!, economy))
    }

    @Test
    fun `переключатель демо меняет профиль и игру, прогресс остаётся`() = runBlocking {
        val store = GameStore(game, FakeStorage())
        val look = PetAppearance(PetCharacter.PUSHISTIK, BodyColor.A, EyesVariant.ROUND)
        val id = (store.createProfile("Финни", look) as ProfileResult.Saved).profileId
        store.execute(id) { selectGoal(it, "bike") }

        store.setDemo(id, true)
        val on = store.observeGame(id).first()!!
        assertTrue(on.profile.isDemo)
        assertTrue(on.state.isDemo)
        assertEquals("bike", on.state.activeGoalId)

        store.setDemo(id, false)
        assertFalse(store.observeGame(id).first()!!.state.isDemo)
    }

    @Test
    fun `настоящий контент — 6 уровней, стадия равна уровню, в демо все за 5 уровней`() {
        val content = ContentParser.parse { File("src/main/assets/content", it).readText() }
        val real = Game(content, Random(1)) { now }
        assertEquals(6, content.economy.maxLevel)

        var s = real.newGame(isDemo = true)
        val stages = mutableListOf(real.stage(s))
        repeat(5) {
            val withGoal = if (s.activeGoalId == null) real.selectGoal(s, content.goals.first().id).state() else s
            s = real.closePeriod(real.confirmPlan(withGoal, needs = 5, wants = 5, savings = 5).state()).state()
            assertEquals(real.level(s), real.stage(s))
            stages += real.stage(s)
        }
        assertEquals(6, real.level(s))
        assertEquals(listOf(1, 2, 3, 4, 5, 6), stages)
    }
}
