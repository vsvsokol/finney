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
 * Сон от 0 до 100 — час (в демо — 10 секунд): спит столько, сколько не хватает до 100,
 * сон растёт постепенно, будить можно раньше.
 */
class SleepTest {

    private var now = 0L
    private val game = Game(Fixtures.content) { now }
    private val hour = 60 * 60_000L

    private fun tired() = game.newGame().also { assertTrue(it.pet.energy < PetRules.STAT_MAX) }

    private fun withEnergy(energy: Int) = game.newGame().let { it.copy(pet = it.pet.copy(energy = energy)) }

    @Test
    fun `лёг спать — бесплатно, до подтверждения плана, сон растёт 100 за час`() {
        val before = withEnergy(40)
        val asleep = game.sleep(before).state()

        assertEquals(before.balance, asleep.balance)
        assertEquals(before.ledger, asleep.ledger)
        assertEquals(40, game.energyAt(asleep, now))

        now += hour / 4
        assertEquals(65, game.energyAt(asleep, now))

        now += hour
        assertEquals(PetRules.STAT_MAX, game.energyAt(asleep, now))
    }

    @Test
    fun `время сна зависит от шкалы — с нуля час, с 37 — 63 процента часа`() {
        assertEquals(hour, game.sleepMillis(withEnergy(0)))
        assertEquals(hour * 63 / 100, game.sleepMillis(withEnergy(37)))
        assertEquals(hour / 100, game.sleepMillis(withEnergy(99)))
    }

    @Test
    fun `разбудили раньше — сон сколько успел набрать`() {
        val asleep = game.sleep(withEnergy(20)).state()
        now += hour / 4
        val woke = game.wake(asleep).state()

        assertNull(woke.sleepingSince)
        assertEquals(45, woke.pet.energy)
    }

    @Test
    fun `просыпается сам, когда сон дорос до 100`() {
        val asleep = game.sleep(withEnergy(37)).state()
        val left = hour * 63 / 100
        assertEquals(asleep.sleepingSince!! + left, game.sleepEndsAt(asleep))

        now += left - 1
        assertEquals(asleep, game.settleSleep(asleep))
        now += 1
        val settled = game.settleSleep(asleep)
        assertNull(settled.sleepingSince)
        assertEquals(PetRules.STAT_MAX, settled.pet.energy)
    }

    @Test
    fun `в демо-режиме сон от 0 до 100 — 10 секунд`() {
        val demo = game.newGame(isDemo = true)
        val asleep = game.sleep(demo.copy(pet = demo.pet.copy(energy = 50))).state()
        assertEquals(10_000L, game.fullSleepMillis(asleep))
        assertEquals(5_000L, game.sleepMillis(asleep))
        now += 5_000L
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
