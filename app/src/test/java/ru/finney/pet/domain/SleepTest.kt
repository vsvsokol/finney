package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.pet.PetRules

/**
 * Сон — третья потребность: убывает за период, восстанавливается бесплатно сном в капсуле.
 * Полный сон — час (в демо — 10 секунд), сон растёт постепенно, будить можно раньше.
 */
class SleepTest {

    private var now = 0L
    private val game = Game(Fixtures.content) { now }
    private val hour = 60 * 60_000L

    private fun tired() = game.newGame().also { assertTrue(it.pet.energy < PetRules.STAT_MAX) }

    @Test
    fun `лёг спать — бесплатно, до подтверждения плана, сон растёт за час до 100`() {
        val before = tired()
        val asleep = game.sleep(before).state()

        assertEquals(before.balance, asleep.balance)
        assertEquals(before.ledger, asleep.ledger)
        assertEquals(before.pet.energy, game.energyAt(asleep, now))

        now += hour / 2
        val half = before.pet.energy + (PetRules.STAT_MAX - before.pet.energy) / 2
        assertEquals(half, game.energyAt(asleep, now))

        now += hour
        assertEquals(PetRules.STAT_MAX, game.energyAt(asleep, now))
    }

    @Test
    fun `разбудили раньше — сон сколько успел набрать`() {
        val before = tired()
        val asleep = game.sleep(before).state()
        now += hour / 4
        val woke = game.wake(asleep).state()

        assertNull(woke.sleepingSince)
        assertEquals(before.pet.energy + (PetRules.STAT_MAX - before.pet.energy) / 4, woke.pet.energy)
    }

    @Test
    fun `через час просыпается сам`() {
        val asleep = game.sleep(tired()).state()
        now += hour - 1
        assertEquals(asleep, game.settleSleep(asleep))
        now += 1
        val settled = game.settleSleep(asleep)
        assertNull(settled.sleepingSince)
        assertEquals(PetRules.STAT_MAX, settled.pet.energy)
    }

    @Test
    fun `в демо-режиме полный сон — 10 секунд`() {
        val asleep = game.sleep(game.newGame(isDemo = true)).state()
        assertEquals(10_000L, game.sleepMillis(asleep))
        now += 10_000L
        assertEquals(PetRules.STAT_MAX, game.energyAt(asleep, now))
    }

    @Test
    fun `пока спит — только ждать или будить`() {
        val asleep = game.sleep(game.confirmPlan(tired(), 0, 0, 0).state()).state()

        assertEquals(Rejection.Asleep, game.buy(asleep, "apple").reason())
        assertEquals(Rejection.Asleep, game.closePeriod(asleep).reason())
        assertEquals(Rejection.Asleep, game.deposit(asleep, 5).reason())
        assertEquals(Rejection.Asleep, game.sleep(asleep).reason())
        assertEquals(Rejection.NotAsleep, game.wake(game.wake(asleep).state()).reason())
    }

    @Test
    fun `выспавшегося не уложить`() {
        val rested = game.sleepFully(tired())
        assertEquals(PetRules.STAT_MAX, rested.pet.energy)
        assertEquals(Rejection.NotSleepy, game.sleep(rested).reason())
    }

    @Test
    fun `за период сон падает ниже порога — спать нужно каждый период`() {
        val threshold = Fixtures.economy.pet.needsThreshold
        val rested = game.sleepFully(game.confirmPlan(tired(), 0, 0, 0).state())
        val next = game.closePeriod(rested).state()
        assertTrue("после периода снова хочется спать: ${next.pet.energy}", next.pet.energy < threshold)
    }

    @Test
    fun `без сна нужное не закрыто и очков за него нет`() {
        var s = game.coverNeeds(game.confirmPlan(tired(), needs = 40, wants = 0, savings = 0).state())
        s = s.copy(pet = s.pet.copy(energy = 0))
        val result = game.closePeriod(s).state().periods.first().result!!
        assertFalse(result.needsCovered)
    }
}
