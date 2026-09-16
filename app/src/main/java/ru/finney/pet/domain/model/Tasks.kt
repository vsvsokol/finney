package ru.finney.pet.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
enum class TaskTheme {
    @SerialName("planning") PLANNING,
    @SerialName("savings") SAVINGS,
    @SerialName("shopping") SHOPPING,
}

enum class TaskOutcome { SUCCESS, FAIL }

/** Задание из tasks.json. Движок выбирается полем `engine`, схемы — docs/economy.md, раздел 9. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("engine")
sealed class TaskDefinition {
    abstract val id: String
    abstract val theme: TaskTheme
    abstract val title: String
    abstract val intro: String
    abstract val explainOk: String
    abstract val explainFail: String

    /** Переопределяет награду из economy.json. */
    abstract val reward: TaskReward?

    /** С какого периода задание открыто. В демо-режиме игнорируется. */
    abstract val unlockPeriod: Int
}

@Serializable
@SerialName("distributor")
data class DistributorTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    val amount: Int,
    val baskets: List<TaskBasket>,
    val rules: List<DistributorRule> = emptyList(),
    val goal: TaskGoalPreview? = null,
) : TaskDefinition()

@Serializable
data class TaskBasket(val id: String, val label: String, val hint: String? = null)

/** Цель, на которую показывается влияние суммы в корзине [basket]. */
@Serializable
data class TaskGoalPreview(val label: String, val price: Int, val saved: Int, val basket: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class DistributorRule {
    abstract val basket: String

    @Serializable
    @SerialName("min")
    data class Min(override val basket: String, val value: Int) : DistributorRule()

    @Serializable
    @SerialName("max")
    data class Max(override val basket: String, val value: Int) : DistributorRule()
}

@Serializable
@SerialName("basket")
data class BasketTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    val limit: Int,
    val shelf: List<ShelfItem>,
    val preloaded: List<String> = emptyList(),
    val rules: List<BasketRule> = emptyList(),
) : TaskDefinition()

@Serializable
data class ShelfItem(val id: String, val label: String, val price: Int, val category: Category)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class BasketRule {
    @Serializable
    @SerialName("hasCategory")
    data class HasCategory(val category: Category) : BasketRule()

    @Serializable
    @SerialName("includes")
    data class Includes(val item: String) : BasketRule()

    @Serializable
    @SerialName("excludes")
    data class Excludes(val item: String) : BasketRule()
}

@Serializable
@SerialName("goal_slider")
data class GoalSliderTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    val goalPrice: Int,
    val periods: Int,
    val incomePerPeriod: Int,
    val step: Int = 5,
) : TaskDefinition()
