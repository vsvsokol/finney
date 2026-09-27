package ru.finney.pet.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.Fixtures
import ru.finney.pet.domain.model.PetStats

class ReminderTextsTest {

    private val rule = Fixtures.economy.pet
    private val fine = PetStats(satiety = 80, hygiene = 80, mood = 80, energy = 80)

    @Test
    fun `текст по тому, чего хочется больше всего — еда, чистота, сон, радость`() {
        assertEquals(PetWish.FINE, ReminderTexts.wish(fine, rule))
        assertEquals(PetWish.HUNGRY, ReminderTexts.wish(fine.copy(satiety = 10, hygiene = 10), rule))
        assertEquals(PetWish.DIRTY, ReminderTexts.wish(fine.copy(hygiene = 10, energy = 10), rule))
        assertEquals(PetWish.TIRED, ReminderTexts.wish(fine.copy(energy = 10, mood = 10), rule))
        assertEquals(PetWish.BORED, ReminderTexts.wish(fine.copy(mood = 10), rule))
    }

    @Test
    fun `порог — «нужное обеспечено»`() {
        assertEquals(PetWish.FINE, ReminderTexts.wish(fine.copy(satiety = rule.needsThreshold), rule))
        assertEquals(PetWish.HUNGRY, ReminderTexts.wish(fine.copy(satiety = rule.needsThreshold - 1), rule))
    }

    @Test
    fun `в каждом тексте имя питомца и никакого давления`() {
        val texts = PetWish.entries.map { ReminderTexts.reminder("Бублик", it) } + ReminderTexts.woke("Бублик")
        val pressure = listOf("плох", "умр", "бол", "срочно", "бросил", "забыл", "!!")
        for (text in texts) {
            assertTrue(text.title, text.title.contains("Бублик"))
            val all = (text.title + " " + text.text).lowercase()
            pressure.forEach { assertFalse("«$it» в «$all»", all.contains(it)) }
        }
        assertEquals("все тексты разные", texts.size, texts.map { it.title }.toSet().size)
    }
}
