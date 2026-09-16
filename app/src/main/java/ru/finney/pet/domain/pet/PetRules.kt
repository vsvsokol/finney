package ru.finney.pet.domain.pet

import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.PetRule
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.ShopItem
import ru.finney.pet.domain.model.StatEffect

enum class Emotion { HUNGRY, DIRTY, SAD, HAPPY, CALM }

/** Шкалы и эмоция питомца. docs/economy.md, раздел 5. */
object PetRules {
    const val STAT_MIN = 0
    const val STAT_MAX = 100

    fun decay(stats: PetStats, decay: StatEffect): PetStats = PetStats(
        satiety = clamp(stats.satiety - decay.satiety),
        hygiene = clamp(stats.hygiene - decay.hygiene),
        mood = clamp(stats.mood - decay.mood),
    )

    fun apply(stats: PetStats, effect: StatEffect): PetStats = PetStats(
        satiety = clamp(stats.satiety + effect.satiety),
        hygiene = clamp(stats.hygiene + effect.hygiene),
        mood = clamp(stats.mood + effect.mood),
    )

    fun needsCovered(stats: PetStats, rule: PetRule): Boolean =
        stats.satiety >= rule.needsThreshold && stats.hygiene >= rule.needsThreshold

    /** Проверка сверху вниз, первое совпадение. */
    fun emotion(stats: PetStats, rule: PetRule): Emotion = when {
        stats.satiety < rule.emotionLow -> Emotion.HUNGRY
        stats.hygiene < rule.emotionLow -> Emotion.DIRTY
        stats.mood < rule.emotionLow -> Emotion.SAD
        minOf(stats.satiety, stats.hygiene, stats.mood) >= rule.emotionHappy -> Emotion.HAPPY
        else -> Emotion.CALM
    }

    /**
     * Минимальная стоимость довести сытость и чистоту до [threshold] обязательными товарами.
     * Подсказка на экране плана. Шкалы считаются независимо: товар, влияющий на обе,
     * учитывается в каждой — для текущего магазина таких нет.
     * null — если в магазине нет товара, поднимающего нужную шкалу.
     */
    fun needsCost(stats: PetStats, shop: List<ShopItem>, threshold: Int): Int? {
        val needs = shop.filter { it.category == Category.NEEDS }
        val satiety = minCost(threshold - stats.satiety, needs.map { it.effect.satiety to it.price })
        val hygiene = minCost(threshold - stats.hygiene, needs.map { it.effect.hygiene to it.price })
        return if (satiety == null || hygiene == null) null else satiety + hygiene
    }

    /** Задача о рюкзаке без ограничения количества: набрать прирост ≥ deficit за минимальную цену. */
    private fun minCost(deficit: Int, options: List<Pair<Int, Int>>): Int? {
        if (deficit <= 0) return 0
        val useful = options.filter { (gain, _) -> gain > 0 }
        if (useful.isEmpty()) return null
        val best = IntArray(deficit + 1) { Int.MAX_VALUE }
        best[0] = 0
        for (d in 1..deficit) {
            for ((gain, price) in useful) {
                val previous = best[maxOf(0, d - gain)]
                if (previous != Int.MAX_VALUE) best[d] = minOf(best[d], previous + price)
            }
        }
        return best[deficit]
    }

    private fun clamp(value: Int): Int = value.coerceIn(STAT_MIN, STAT_MAX)
}
