package ru.finney.pet.content

import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ChangeMode
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.tasks.TaskEngines

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
            val at = "задание ${task.id}"
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
                    task.shelf.filter { it.qty <= 0 }.forEach { add("$at — ${it.id}: qty должно быть > 0") }
                    task.preloaded.filter { it !in shelf }.forEach { add("$at — preloaded ссылается на $it") }
                    task.rules.forEach { rule ->
                        val items = when (rule) {
                            is BasketRule.Includes -> listOf(rule.item)
                            is BasketRule.Excludes -> listOf(rule.item)
                            is BasketRule.AnyOf -> rule.items
                            is BasketRule.MinQty -> rule.items
                            is BasketRule.HasCategory -> emptyList()
                        }
                        items.filter { it !in shelf }.forEach { add("$at — правило ссылается на $it") }
                        if (rule is BasketRule.AnyOf && rule.items.isEmpty()) add("$at — в правиле anyOf пустой список")
                        if (rule is BasketRule.MinQty && rule.qty <= 0) add("$at — в правиле minQty qty должно быть > 0")
                    }
                }
                is GoalSliderTask -> {
                    if (task.goalPrice <= 0 || task.periods <= 0 || task.incomePerPeriod <= 0 || task.step <= 0) {
                        add("$at — goalPrice, periods, incomePerPeriod и step должны быть > 0")
                    }
                }
                is SorterTask -> checkSorter(task, at)
                is GoalRaceTask -> checkRace(task, at)
                is ReserveTask -> checkReserve(task, at)
                is StandTask -> checkStand(task, at)
                is ChangeTask -> checkChange(task, at)
            }
        }
    }

    // ---------- Мини-игры: кроме чисел проверяем, что игру можно и выиграть ----------

    private fun MutableList<String>.checkSorter(task: SorterTask, at: String) {
        if (task.items.isEmpty()) add("$at — items пустой")
        duplicates(task.items.map { it.id }).forEach { add("$at — повторяется вещь $it") }
        task.items.filter { it.why.isBlank() }.forEach { add("$at — ${it.id}: пустое why") }
        if (task.minCorrect !in 1..task.items.size) add("$at — minCorrect должен быть от 1 до ${task.items.size}")
    }

    private fun MutableList<String>.checkRace(task: GoalRaceTask, at: String) {
        if (task.days <= 0 || task.incomePerDay <= 0 || task.step <= 0 || task.goal.price <= 0) {
            add("$at — days, incomePerDay, step и goal.price должны быть > 0")
            return
        }
        if (task.incomePerDay % task.step != 0) add("$at — incomePerDay должен делиться на step")
        if (task.startSaved < 0) add("$at — startSaved не может быть < 0")
        if (task.startSaved + task.days * task.incomePerDay < task.goal.price) {
            add("$at — цель не набрать, даже если откладывать всё")
        }
        task.events.filter { it.day !in 1..task.days }.forEach { add("$at — событие «${it.label}» в дне ${it.day}, а дней ${task.days}") }
        task.events.filter { it.price <= 0 }.forEach { add("$at — событие «${it.label}»: цена должна быть > 0") }
    }

    private fun MutableList<String>.checkReserve(task: ReserveTask, at: String) {
        if (task.amount <= 0) add("$at — amount должен быть > 0")
        if (task.surprise.price <= 0) add("$at — surprise.price должен быть > 0")
        duplicates(task.spendings.map { it.id }).forEach { add("$at — повторяется трата $it") }
        task.spendings.filter { it.price <= 0 }.forEach { add("$at — ${it.id}: цена должна быть > 0") }
        val needs = task.spendings.filter { it.category == Category.NEEDS }.sumOf { it.price }
        if (task.amount - needs < task.surprise.price) {
            add("$at — после нужного не остаётся запаса на «${task.surprise.label}»: игру не выиграть")
        }
    }

    private fun MutableList<String>.checkStand(task: StandTask, at: String) {
        val i = task.ingredient
        if (task.budget <= 0 || i.price <= 0 || i.yields <= 0 || task.cupPrice <= 0 || task.guests <= 0) {
            add("$at — budget, ingredient.price, ingredient.yields, cupPrice и guests должны быть > 0")
            return
        }
        if (TaskEngines.bestStock(task) * i.price > task.budget) add("$at — на всех гостей не хватает budget")
    }

    private fun MutableList<String>.checkChange(task: ChangeTask, at: String) {
        if (task.coins.isEmpty() || task.coins.any { it <= 0 }) add("$at — coins: нужны монеты > 0")
        if (task.rounds.isEmpty()) add("$at — rounds пустой")
        task.minCorrect?.let { if (it !in 1..task.rounds.size) add("$at — minCorrect должен быть от 1 до ${task.rounds.size}") }
        task.rounds.forEachIndexed { n, round ->
            val r = "$at, раунд ${n + 1}"
            when (round.mode) {
                ChangeMode.GIVE -> {
                    val paid = round.paid
                    if (paid == null || paid <= round.price) add("$r — paid должен быть больше price")
                    else if (!reachable(round.target, task.coins)) add("$r — сдачу ${round.target} не набрать монетами ${task.coins}")
                }
                ChangeMode.EXACT -> {
                    round.wallet.filter { it !in task.coins }.forEach { add("$r — в кошельке монета $it, которой нет в coins") }
                    if (!payable(round.price, round.wallet)) add("$r — ${round.price} не набрать из кошелька ${round.wallet}")
                }
            }
        }
    }

    /** Набирается ли [sum] монетами [coins], каждой сколько угодно. */
    private fun reachable(sum: Int, coins: List<Int>): Boolean {
        if (sum <= 0) return false
        val ok = BooleanArray(sum + 1).also { it[0] = true }
        for (s in 1..sum) ok[s] = coins.any { it in 1..s && ok[s - it] }
        return ok[sum]
    }

    /** Набирается ли [sum] монетами из [wallet], каждой не больше одного раза. */
    private fun payable(sum: Int, wallet: List<Int>): Boolean {
        if (sum <= 0) return false
        val ok = BooleanArray(sum + 1).also { it[0] = true }
        for (coin in wallet) for (s in sum downTo coin) if (ok[s - coin]) ok[s] = true
        return ok[sum]
    }

    private fun duplicates(ids: List<String>): Set<String> =
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
