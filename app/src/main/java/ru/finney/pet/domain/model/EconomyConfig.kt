package ru.finney.pet.domain.model

import kotlinx.serialization.Serializable

/** economy.json. Смысл каждого числа — docs/economy.md. Стадии нумеруются с 1. */
@Serializable
data class EconomyConfig(
    val incomeByStage: List<Int>,
    val taskReward: TaskReward,
    val parentBonus: ParentBonusRule,
    val planTolerance: Int,
    val points: PointsRule,
    val pointsPerLevel: Int,
    val maxLevel: Int,
    val stageStartLevels: List<Int>,
    val pet: PetRule,
) {
    fun income(stage: Int): Int = incomeByStage[stage - 1]
    fun decay(stage: Int): StatEffect = pet.decayByStage[stage - 1]
}

@Serializable
data class TaskReward(val success: Int, val fail: Int)

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
)
