package ru.finney.pet.domain.progress

import ru.finney.pet.domain.model.EconomyConfig
import ru.finney.pet.domain.model.PointsRule

/** Очки, уровни, стадии. docs/economy.md, раздел 8. */
object Progression {

    fun level(points: Int, config: EconomyConfig): Int =
        minOf(config.maxLevel, 1 + points / config.pointsPerLevel)

    fun stage(level: Int, config: EconomyConfig): Int =
        config.stageStartLevels.count { it <= level }.coerceAtLeast(1)

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
