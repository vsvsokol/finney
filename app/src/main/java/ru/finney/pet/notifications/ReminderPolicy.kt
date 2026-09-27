package ru.finney.pet.notifications

import ru.finney.pet.domain.model.ReminderRule
import java.time.Duration
import java.time.LocalDateTime

/** Что делать с напоминанием, которое подошло по времени. */
sealed interface ReminderDecision {
    data object Send : ReminderDecision

    /** Ночь: перенести на [at] — утро. */
    data class Later(val at: LocalDateTime) : ReminderDecision

    /** Приложение открывали недавно — это напоминание пропустить, следующее по расписанию. */
    data object Skip : ReminderDecision
}

/**
 * Когда напоминать. Чистые функции: время приходит снаружи, поэтому правило проверяется
 * тестами без часов и без Android. Числа — [ReminderRule] в economy.json.
 */
object ReminderPolicy {

    /** Раз в сколько напоминать: раз в сутки, в демо-режиме — раз в несколько минут. */
    fun interval(rule: ReminderRule, isDemo: Boolean): Duration =
        if (isDemo) Duration.ofMinutes(rule.demoReminderMinutes.toLong()) else Duration.ofHours(rule.everyHours.toLong())

    /** Тихие часы: с [ReminderRule.quietFromHour] вечера до [ReminderRule.quietToHour] утра. */
    fun isQuiet(time: LocalDateTime, rule: ReminderRule): Boolean {
        val hour = time.hour
        return if (rule.quietFromHour > rule.quietToHour) {
            hour >= rule.quietFromHour || hour < rule.quietToHour
        } else {
            hour in rule.quietFromHour until rule.quietToHour
        }
    }

    /** Ближайшее время не раньше [time], когда писать уже можно: вне тихих часов. */
    fun nextAllowed(time: LocalDateTime, rule: ReminderRule): LocalDateTime {
        if (!isQuiet(time, rule)) return time
        val morning = time.toLocalDate().atTime(rule.quietToHour, 0)
        return if (time.isBefore(morning)) morning else morning.plusDays(1)
    }

    /**
     * Напоминание подошло по времени [now]: отправить, перенести на утро или пропустить.
     * [lastOpened] — когда приложение последний раз открывали; null — не знаем.
     * В демо-режиме тихих часов нет: эксперт проверяет когда удобно, а ребёнок ночью демо не включает.
     */
    fun decide(now: LocalDateTime, lastOpened: LocalDateTime?, isDemo: Boolean, rule: ReminderRule): ReminderDecision {
        val recent = if (isDemo) Duration.ofMinutes(rule.demoSkipIfOpenedMinutes.toLong()) else Duration.ofHours(rule.skipIfOpenedHours.toLong())
        if (lastOpened != null && Duration.between(lastOpened, now) < recent) return ReminderDecision.Skip
        if (!isDemo && isQuiet(now, rule)) return ReminderDecision.Later(nextAllowed(now, rule))
        return ReminderDecision.Send
    }
}
