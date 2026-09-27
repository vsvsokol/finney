package ru.finney.pet.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.model.ReminderRule
import java.time.Duration
import java.time.LocalDateTime

class ReminderPolicyTest {

    private val rule = ReminderRule(
        everyHours = 24,
        demoReminderMinutes = 2,
        quietFromHour = 21,
        quietToHour = 9,
        skipIfOpenedHours = 6,
        demoSkipIfOpenedMinutes = 1,
    )

    private fun at(hour: Int, minute: Int = 0, day: Int = 10) = LocalDateTime.of(2026, 9, day, hour, minute)

    @Test
    fun `тихие часы — с 21 00 до 9 00`() {
        assertFalse(ReminderPolicy.isQuiet(at(20, 59), rule))
        assertTrue(ReminderPolicy.isQuiet(at(21, 0), rule))
        assertTrue(ReminderPolicy.isQuiet(at(0, 30), rule))
        assertTrue(ReminderPolicy.isQuiet(at(8, 59), rule))
        assertFalse(ReminderPolicy.isQuiet(at(9, 0), rule))
        assertFalse(ReminderPolicy.isQuiet(at(14, 0), rule))
    }

    @Test
    fun `ночное напоминание переносится на 9 утра`() {
        assertEquals(at(9, day = 11), ReminderPolicy.nextAllowed(at(22, 15), rule))
        assertEquals(at(9, day = 10), ReminderPolicy.nextAllowed(at(3, 0), rule))
        assertEquals(at(14, 0), ReminderPolicy.nextAllowed(at(14, 0), rule))
        assertEquals(ReminderDecision.Later(at(9, day = 11)), ReminderPolicy.decide(at(21, 30), null, isDemo = false, rule))
    }

    @Test
    fun `днём и давно не открывали — отправить`() {
        assertEquals(ReminderDecision.Send, ReminderPolicy.decide(at(15), lastOpened = at(8), isDemo = false, rule))
        assertEquals(ReminderDecision.Send, ReminderPolicy.decide(at(15), lastOpened = null, isDemo = false, rule))
    }

    @Test
    fun `открывали недавно — не писать`() {
        assertEquals(ReminderDecision.Skip, ReminderPolicy.decide(at(15), lastOpened = at(10), isDemo = false, rule))
        assertEquals(ReminderDecision.Skip, ReminderPolicy.decide(at(15), lastOpened = at(14, 59, day = 10).plusSeconds(30), isDemo = true, rule))
        assertEquals(ReminderDecision.Send, ReminderPolicy.decide(at(15), lastOpened = at(14, 58), isDemo = true, rule))
    }

    @Test
    fun `в демо часто и без тихих часов, обычный режим — раз в сутки`() {
        assertEquals(Duration.ofMinutes(2), ReminderPolicy.interval(rule, isDemo = true))
        assertEquals(Duration.ofHours(24), ReminderPolicy.interval(rule, isDemo = false))
        assertEquals(ReminderDecision.Send, ReminderPolicy.decide(at(23), lastOpened = at(22), isDemo = true, rule))
    }

    @Test
    fun `тихие часы в пределах одних суток тоже работают`() {
        val day = rule.copy(quietFromHour = 13, quietToHour = 15)
        assertTrue(ReminderPolicy.isQuiet(at(14), day))
        assertFalse(ReminderPolicy.isQuiet(at(15), day))
        assertEquals(at(15), ReminderPolicy.nextAllowed(at(13, 30), day))
    }
}
