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

/**
 * Товар на полке задания. [qty] — сколько штук в упаковке: «5 яблок за 20» выгоднее «2 за 10».
 * [art] и [emoji] — только для рисунка, см. [ItemArt].
 */
@Serializable
data class ShelfItem(
    val id: String,
    val label: String,
    val price: Int,
    val category: Category,
    val qty: Int = 1,
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class BasketRule {
    /** Строка списка покупок. null — правило проверяется, но в списке не показывается. */
    open val label: String? get() = null

    @Serializable
    @SerialName("hasCategory")
    data class HasCategory(val category: Category) : BasketRule()

    @Serializable
    @SerialName("includes")
    data class Includes(val item: String) : BasketRule()

    @Serializable
    @SerialName("excludes")
    data class Excludes(val item: String) : BasketRule()

    /** Взят хотя бы один из [items]: «мыло» — брусок или жидкое. */
    @Serializable
    @SerialName("anyOf")
    data class AnyOf(val items: List<String>, override val label: String? = null) : BasketRule()

    /** Штук из [items] вместе не меньше [qty]: «4 яблока на 2 дня». */
    @Serializable
    @SerialName("minQty")
    data class MinQty(val items: List<String>, val qty: Int, override val label: String? = null) : BasketRule()
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

// ---------- Мини-игры ----------
//
// Мини-игра — это задание со своим движком: награда, попытки и прогресс у неё общие
// со всеми заданиями. Живут в assets/content/tasks.json, правила — docs/minigames.md.

/**
 * Рисунок предмета в мини-игре. [art] — имя картинки из res/drawable-nodpi без расширения
 * (`item_food_apple`), [emoji] — если картинки нет. Нет ни того, ни другого — первая буква названия.
 */
interface ItemArt {
    val art: String?
    val emoji: String?
}

/** «Конвейер»: разложить вещи на нужное и желаемое. */
@Serializable
@SerialName("sorter")
data class SorterTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    val items: List<SortItem>,
    /** Сколько вещей нужно разложить верно для успеха. */
    val minCorrect: Int,
) : TaskDefinition()

/** [why] — одна фраза, которую ребёнок видит, если отнёс вещь не туда. */
@Serializable
data class SortItem(
    val id: String,
    val label: String,
    val category: Category,
    val why: String,
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt

/** «Дорога к цели»: каждый день решить, сколько из дохода отложить. */
@Serializable
@SerialName("goal_race")
data class GoalRaceTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    val goal: RaceGoal,
    val days: Int,
    val incomePerDay: Int,
    val step: Int = 5,
    /** Сколько уже лежит в копилке на старте. */
    val startSaved: Int = 0,
    val events: List<RaceEvent> = emptyList(),
) : TaskDefinition()

@Serializable
data class RaceGoal(
    val label: String,
    val price: Int,
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt

/** Соблазн дня [day] (с 1). Взят, если в этот день потрачено не меньше [price]. */
@Serializable
data class RaceEvent(
    val day: Int,
    val label: String,
    val price: Int,
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt

/** «Дождливый день»: спланировать траты с запасом, потом случается непредвиденное. */
@Serializable
@SerialName("reserve")
data class ReserveTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    val amount: Int,
    val spendings: List<Spending>,
    val surprise: Surprise,
) : TaskDefinition()

/** Трата недели. Нужное нельзя ни убрать из плана, ни перенести. */
@Serializable
data class Spending(
    val id: String,
    val label: String,
    val price: Int,
    val category: Category,
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt

@Serializable
data class Surprise(
    val label: String,
    val text: String,
    val price: Int,
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt

/** «Лимонадная лавка»: закупить сырьё под число гостей. */
@Serializable
@SerialName("stand")
data class StandTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    /** Сколько денег на закупку. */
    val budget: Int,
    val ingredient: StandIngredient,
    /** Цена одного стакана для гостя. */
    val cupPrice: Int,
    val guests: Int,
    /** Подсказка о погоде и числе гостей: «Жарко — придут около 8 гостей». */
    val forecast: String,
) : TaskDefinition()

/** Из одной штуки выходит [yields] стаканов. */
@Serializable
data class StandIngredient(
    val label: String,
    val price: Int,
    val yields: Int,
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt

/** «Касса»: отсчитать сдачу или заплатить без сдачи. */
@Serializable
@SerialName("change")
data class ChangeTask(
    override val id: String,
    override val theme: TaskTheme,
    override val title: String,
    override val intro: String,
    override val explainOk: String,
    override val explainFail: String,
    override val reward: TaskReward? = null,
    override val unlockPeriod: Int = 1,
    /** Номиналы монет в ящике кассы. */
    val coins: List<Int>,
    val rounds: List<ChangeRound>,
    /** Сколько раундов нужно решить верно с первой попытки. По умолчанию — все. */
    val minCorrect: Int? = null,
) : TaskDefinition()

@Serializable
enum class ChangeMode {
    /** Покупатель дал [ChangeRound.paid], ребёнок отсчитывает сдачу из ящика. */
    @SerialName("give") GIVE,

    /** Финни платит ровно [ChangeRound.price] монетами из кошелька [ChangeRound.wallet]. */
    @SerialName("exact") EXACT,
}

@Serializable
data class ChangeRound(
    val mode: ChangeMode,
    val label: String,
    val price: Int,
    val paid: Int? = null,
    val wallet: List<Int> = emptyList(),
    override val art: String? = null,
    override val emoji: String? = null,
) : ItemArt {
    /** Сколько монет нужно положить на кассу. */
    val target: Int get() = if (mode == ChangeMode.GIVE) (paid ?: 0) - price else price
}
