package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.StatEffect
import ru.finney.pet.domain.pet.Emotion
import ru.finney.pet.domain.pet.PetRules

/** docs/economy.md, раздел 5. */
class PetRulesTest {

    private val rule = Fixtures.economy.pet

    @Test
    fun `10 шкалы не выходят за 0 и 100`() {
        assertEquals(PetStats(0, 0, 0), PetRules.decay(PetStats(10, 20, 5), StatEffect(60, 40, 30)))
        assertEquals(PetStats(100, 100, 100), PetRules.apply(PetStats(90, 95, 99), StatEffect(45, 50, 30)))
    }

    @Test
    fun `эмоция — первое совпадение сверху вниз`() {
        assertEquals(Emotion.HUNGRY, PetRules.emotion(PetStats(10, 10, 10), rule))
        assertEquals(Emotion.DIRTY, PetRules.emotion(PetStats(50, 10, 10), rule))
        assertEquals(Emotion.SAD, PetRules.emotion(PetStats(50, 50, 10), rule))
        assertEquals(Emotion.HAPPY, PetRules.emotion(PetStats(60, 60, 60), rule))
        assertEquals(Emotion.CALM, PetRules.emotion(PetStats(60, 59, 60), rule))
    }

    @Test
    fun `подсказка по нужному — минимальная стоимость до порога`() {
        val shop = Fixtures.shop
        assertEquals(0, PetRules.needsCost(PetStats(50, 50, 0), shop, 50))
        assertEquals(20, PetRules.needsCost(PetStats(30, 40, 0), shop, 50)) // яблоко + мыло
        assertEquals(45, PetRules.needsCost(PetStats(0, 0, 0), shop, 50)) // миска + яблоко + полотенце
        assertNull(PetRules.needsCost(PetStats(0, 0, 0), shop.filter { it.effect.hygiene == 0 }, 50))
    }
}
