package ru.finney.pet.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    @SerialName("needs") NEEDS,
    @SerialName("wants") WANTS,
}

@Serializable
enum class ItemKind {
    /** Еда, уход, лакомства: покупается сколько угодно раз. */
    @SerialName("consumable") CONSUMABLE,

    /** Шапка, шарф, очки: покупается один раз и остаётся на питомце. */
    @SerialName("accessory") ACCESSORY,
}

/** Изменение шкал питомца. В снижении за период значения положительные и вычитаются. */
@Serializable
data class StatEffect(
    val satiety: Int = 0,
    val hygiene: Int = 0,
    val mood: Int = 0,
)

@Serializable
data class ShopItem(
    val id: String,
    val label: String,
    val price: Int,
    val category: Category,
    val kind: ItemKind = ItemKind.CONSUMABLE,
    val effect: StatEffect = StatEffect(),
)

@Serializable
data class Goal(
    val id: String,
    val label: String,
    val price: Int,
    val moodBonus: Int = 0,
)

/** glossary.json — справочник терминов, ТЗ п. 2.5.11. */
@Serializable
data class Glossary(
    @SerialName("_comment") val comment: String? = null,
    val terms: List<GlossaryTerm>,
)

@Serializable
data class GlossaryTerm(val id: String, val term: String, val text: String)

/** Весь учебный контент из assets/content. Загружается и проверяется пакетом `content`. */
data class GameContent(
    val economy: EconomyConfig,
    val shop: List<ShopItem>,
    val goals: List<Goal>,
    val tasks: List<TaskDefinition>,
    val glossary: List<GlossaryTerm> = emptyList(),
) {
    fun item(id: String): ShopItem? = shop.firstOrNull { it.id == id }
    fun goal(id: String): Goal? = goals.firstOrNull { it.id == id }
    fun task(id: String): TaskDefinition? = tasks.firstOrNull { it.id == id }
}
