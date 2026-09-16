package ru.finney.pet.content

import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.GoalSliderTask

/**
 * Проверка ссылок и чисел, которые JSON-схема не ловит. Запускается при загрузке и в unit-тесте,
 * поэтому битый контент ловит CI, а не эксперт.
 */
object ContentValidator {

    fun validate(content: GameContent): List<String> = buildList {
        val e = content.economy
        val stages = e.stageStartLevels.size
        if (e.incomeByStage.size != stages) add("economy.json: incomeByStage — нужно $stages значения, по числу стадий")
        if (e.pet.decayByStage.size != stages) add("economy.json: pet.decayByStage — нужно $stages значения, по числу стадий")
        if (e.stageStartLevels.firstOrNull() != 1) add("economy.json: stageStartLevels должен начинаться с 1")
        if (e.stageStartLevels.zipWithNext().any { (a, b) -> a >= b }) add("economy.json: stageStartLevels должен возрастать")
        if (e.stageStartLevels.any { it > e.maxLevel }) add("economy.json: стадия начинается после maxLevel")
        if (e.pointsPerLevel <= 0) add("economy.json: pointsPerLevel должен быть > 0")
        if (e.parentBonus.step <= 0) add("economy.json: parentBonus.step должен быть > 0")
        if (e.incomeByStage.any { it <= 0 }) add("economy.json: доход должен быть > 0")

        duplicates(content.shop.map { it.id }).forEach { add("shop.json: повторяется id $it") }
        content.shop.filter { it.price <= 0 }.forEach { add("shop.json: ${it.id} — цена должна быть > 0") }
        val needs = content.shop.filter { it.category == Category.NEEDS }
        if (needs.none { it.effect.satiety > 0 }) add("shop.json: нет обязательного товара, поднимающего сытость")
        if (needs.none { it.effect.hygiene > 0 }) add("shop.json: нет обязательного товара, поднимающего чистоту")

        duplicates(content.goals.map { it.id }).forEach { add("goals.json: повторяется id $it") }
        content.goals.filter { it.price <= 0 }.forEach { add("goals.json: ${it.id} — цена должна быть > 0") }

        duplicates(content.glossary.map { it.id }).forEach { add("glossary.json: повторяется id $it") }
        content.glossary.filter { it.term.isBlank() || it.text.isBlank() }.forEach {
            add("glossary.json: ${it.id} — пустой термин или объяснение")
        }

        duplicates(content.tasks.map { it.id }).forEach { add("tasks.json: повторяется id $it") }
        for (task in content.tasks) {
            val at = "tasks.json: ${task.id}"
            if (task.unlockPeriod < 1) add("$at — unlockPeriod должен быть ≥ 1")
            when (task) {
                is DistributorTask -> {
                    val baskets = task.baskets.map { it.id }
                    if (task.amount <= 0) add("$at — amount должен быть > 0")
                    duplicates(baskets).forEach { add("$at — повторяется корзина $it") }
                    task.rules.filter { it.basket !in baskets }.forEach { add("$at — правило ссылается на корзину ${it.basket}") }
                    task.goal?.let { if (it.basket !in baskets) add("$at — goal ссылается на корзину ${it.basket}") }
                }
                is BasketTask -> {
                    val shelf = task.shelf.map { it.id }
                    if (task.limit <= 0) add("$at — limit должен быть > 0")
                    duplicates(shelf).forEach { add("$at — повторяется товар $it") }
                    task.shelf.filter { it.price <= 0 }.forEach { add("$at — ${it.id}: цена должна быть > 0") }
                    task.preloaded.filter { it !in shelf }.forEach { add("$at — preloaded ссылается на $it") }
                    task.rules.forEach { rule ->
                        val item = when (rule) {
                            is BasketRule.Includes -> rule.item
                            is BasketRule.Excludes -> rule.item
                            is BasketRule.HasCategory -> null
                        }
                        if (item != null && item !in shelf) add("$at — правило ссылается на $item")
                    }
                }
                is GoalSliderTask -> {
                    if (task.goalPrice <= 0 || task.periods <= 0 || task.incomePerPeriod <= 0 || task.step <= 0) {
                        add("$at — goalPrice, periods, incomePerPeriod и step должны быть > 0")
                    }
                }
            }
        }
    }

    private fun duplicates(ids: List<String>): Set<String> =
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
