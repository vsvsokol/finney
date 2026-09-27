package ru.finney.pet.domain.model

import kotlinx.serialization.Serializable

/** economy.json. Смысл каждого числа — docs/economy.md. Стадии нумеруются с 1. */
@Serializable
data class EconomyConfig(
    val incomeByStage: List<Int>,
    val taskReward: TaskReward,
    val parentBonus: ParentBonusRule,
    val planTolerance: Int,
    /** В скольких направлениях плана (нужное, желаемое, копилка) должна быть сумма > 0. ТЗ п. 2.5.5 — во всех трёх. */
    val planDirections: Int = 3,
    val points: PointsRule,
    /** Сколько из трёх условий уровня (нужное, план, копилка) нужно выполнить, чтобы его пройти. Игра уровня обязательна сверх них. */
    val conditionsToPass: Int,
    /** То же в демо-режиме: эксперт проходит 6 уровней за несколько минут (docs/economy.md, раздел 8). */
    val demoConditionsToPass: Int = conditionsToPass,
    /** Обязательна ли игра уровня в демо-режиме. Сама игра выдаётся и видна в любом случае. */
    val demoLevelGameRequired: Boolean = true,
    val maxLevel: Int,
    val stageStartLevels: List<Int>,
    val pet: PetRule,
    val play: PlayRule = PlayRule(),
    val reminders: ReminderRule = ReminderRule(),
) {
    fun income(stage: Int): Int = incomeByStage[stage - 1]
    fun decay(stage: Int): StatEffect = pet.decayByStage[stage - 1]
}

@Serializable
/**
 * Награда за задание. [bonus] — сверху к успеху, если движок засчитал бонус (например,
 * в «Дождливом дне» нужное закрыто и желаемое не отменено); выдаётся тоже один раз.
 */
data class TaskReward(val success: Int, val fail: Int, val bonus: Int = 0)

@Serializable
data class ParentBonusRule(val step: Int, val maxPerPeriod: Int)

@Serializable
data class PointsRule(
    val needsCovered: Int,
    val planMatched: Int,
    val savingsAdded: Int,
    val taskSuccess: Int,
    val taskSuccessMaxPerPeriod: Int,
) {
    val maxPerPeriod: Int get() = needsCovered + planMatched + savingsAdded + taskSuccess * taskSuccessMaxPerPeriod
}

@Serializable
data class PetRule(
    val start: PetStats,
    val decayByStage: List<StatEffect>,
    val needsThreshold: Int,
    val emotionLow: Int,
    val emotionHappy: Int,
    /** Полный сон, минуты. */
    val sleepMinutes: Int,
    /** Полный сон в демо-режиме, секунды. */
    val demoSleepSeconds: Int,
)

/**
 * Игра с игрушкой. Каждое «потряхивание» рядом с питомцем — плюс [moodPerShake] к настроению,
 * но за одну сессию игры не больше [sessionMoodCap]: дальше питомец наигрался, и игрушка
 * настроения не прибавляет. Сессия начинается с первой прибавки и длится [sessionMinutes]
 * (в демо-режиме — [demoSessionSeconds] секунд); новый уровень начинает новую сессию сразу.
 */
@Serializable
data class PlayRule(
    val moodPerShake: Int = 2,
    val sessionMoodCap: Int = 20,
    val sessionMinutes: Int = 60,
    val demoSessionSeconds: Int = 30,
)

/**
 * Напоминания о питомце (пакет notifications). Обычно — раз в [everyHours] часов,
 * в демо-режиме — раз в [demoReminderMinutes] минут, чтобы эксперт их увидел.
 * Ночью, с [quietFromHour] до [quietToHour], не пишем — напоминание переносится на утро.
 * Приложение открывали меньше [skipIfOpenedHours] часов назад (в демо — [demoSkipIfOpenedMinutes]
 * минут) — не пишем: ребёнок и так недавно заглядывал.
 */
@Serializable
data class ReminderRule(
    val everyHours: Int = 24,
    val demoReminderMinutes: Int = 2,
    val quietFromHour: Int = 21,
    val quietToHour: Int = 9,
    val skipIfOpenedHours: Int = 6,
    val demoSkipIfOpenedMinutes: Int = 1,
)
