package ru.finney.pet.domain.progress

import ru.finney.pet.domain.model.EconomyConfig
import ru.finney.pet.domain.model.PeriodResult
import ru.finney.pet.domain.model.PointsRule

/**
 * Уровни и стадии. docs/economy.md, раздел 8.
 *
 * Уровень — это игровой период, который ребёнок прошёл: составил план, прошёл игру
 * уровня, позаботился о питомце, отложил — и завершил. Отдельного счёта очков ребёнок не видит: уровень
 * растёт на один за каждый пройденный период и никогда не падает.
 */
object Progression {

    /** Сколько из трёх условий уровня выполнено: нужное обеспечено, план выполнен, отложено в копилку. */
    fun conditionsMet(needsCovered: Boolean, planMatched: Boolean, savingsAdded: Boolean): Int =
        listOf(needsCovered, planMatched, savingsAdded).count { it }

    /**
     * Пройден ли уровень, сыгранный в этом периоде. Решение записано при закрытии
     * ([PeriodResult.passed]); у периодов до базы версии 8 — по обычным правилам:
     * игра уровня обязательна, из остальных — [EconomyConfig.conditionsToPass].
     */
    fun passed(result: PeriodResult, config: EconomyConfig): Boolean =
        result.passed ?: decide(
            result.needsCovered, result.planMatched, result.savingsAdded, result.gamePassed,
            toPass = config.conditionsToPass, gameRequired = true,
        )

    /** Правило прохождения: игра уровня (если обязательна) и [toPass] из трёх условий. */
    fun decide(
        needsCovered: Boolean,
        planMatched: Boolean,
        savingsAdded: Boolean,
        gamePassed: Boolean,
        toPass: Int,
        gameRequired: Boolean,
    ): Boolean = (gamePassed || !gameRequired) && conditionsMet(needsCovered, planMatched, savingsAdded) >= toPass

    /** Уровень после [passedLevels] пройденных периодов. */
    fun level(passedLevels: Int, config: EconomyConfig): Int =
        minOf(config.maxLevel, 1 + passedLevels)

    fun stage(level: Int, config: EconomyConfig): Int =
        config.stageStartLevels.count { it <= level }.coerceAtLeast(1)

    /** Очки периода — только для истории и раздела взрослого; уровень от них не зависит. */
    fun periodPoints(
        needsCovered: Boolean,
        planMatched: Boolean,
        savingsAdded: Boolean,
        successfulTasks: Int,
        rule: PointsRule,
    ): Int {
        var points = 0
        if (needsCovered) points += rule.needsCovered
        if (planMatched) points += rule.planMatched
        if (savingsAdded) points += rule.savingsAdded
        points += minOf(successfulTasks, rule.taskSuccessMaxPerPeriod) * rule.taskSuccess
        return points
    }
}
