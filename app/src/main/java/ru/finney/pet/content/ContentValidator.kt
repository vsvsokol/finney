package ru.finney.pet.content

import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ChangeMode
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.ChoresTask
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.ItemKind
import ru.finney.pet.domain.model.ReceiptTask
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.model.TaskOutcome
import ru.finney.pet.domain.tasks.TaskEngines
import ru.finney.pet.domain.tasks.TaskEvaluation
import ru.finney.pet.domain.tasks.TaskInput

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
        if (e.planDirections !in 0..3) add("economy.json: planDirections должен быть от 0 до 3")
        if (e.conditionsToPass !in 1..3) add("economy.json: conditionsToPass должен быть от 1 до 3")
        if (e.parentBonus.step <= 0) add("economy.json: parentBonus.step должен быть > 0")
        if (e.incomeByStage.any { it <= 0 }) add("economy.json: доход должен быть > 0")
        // Сон не покупают, поэтому в магазине его не проверить: потребность держится только на спаде.
        if (e.pet.decayByStage.any { it.energy <= 0 }) add("economy.json: pet.decayByStage — сон должен убывать за период")
        if (e.pet.sleepMinutes <= 0 || e.pet.demoSleepSeconds <= 0) add("economy.json: pet.sleepMinutes и demoSleepSeconds должны быть > 0")

        duplicates(content.shop.map { it.id }).forEach { add("shop.json: повторяется id $it") }
        content.goals.forEach { goal ->
            val reward = goal.reward ?: return@forEach
            val item = content.item(reward)
            if (item == null) add("goals.json: ${goal.id} — награда $reward не найдена в shop.json")
            else if (item.kind != ItemKind.ACCESSORY) add("goals.json: ${goal.id} — награда $reward должна быть аксессуаром")
        }
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
            if (task.unlockLevel !in 1..e.maxLevel) add("$at — unlockLevel должен быть от 1 до ${e.maxLevel}")
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
                    checkBasketWinnable(task, at)
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
                is ReceiptTask -> checkReceipt(task, at)
                is ChoresTask -> checkChores(task, at)
            }
        }

        for (variants in content.taskSeries) {
            if (variants.size < 2) continue
            val at = "игра ${variants.first().seriesId}"
            if (variants.map { it::class }.distinct().size > 1) add("$at — у вариантов разные движки")
            if (variants.map { it.theme }.distinct().size > 1) add("$at — у вариантов разные темы")
            duplicates(variants.map { it.unlockLevel.toString() }).forEach { add("$at — два варианта с unlockLevel $it") }
        }
    }

    // ---------- Мини-игры: кроме чисел проверяем, что игру можно и выиграть ----------

    /** Перебор корзин: хоть одна укладывается в limit и выполняет список. Полка в игре — до десятка товаров. */
    private fun MutableList<String>.checkBasketWinnable(task: BasketTask, at: String) {
        val shelf = task.shelf
        if (shelf.size > MAX_SHELF) {
            add("$at — на полке больше $MAX_SHELF товаров")
            return
        }
        val winnable = (0 until (1 shl shelf.size)).any { mask ->
            val items = shelf.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            items.sumOf { it.price } <= task.limit &&
                task.rules.all { TaskEngines.ruleMet(task, it, items.map { item -> item.id }.toSet()) }
        }
        if (!winnable) add("$at — ни одна корзина в limit ${task.limit} не выполняет список: игру не выиграть")
    }

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
        duplicates(task.events.map { it.day.toString() }).forEach { add("$at — два соблазна в дне $it") }
        if (task.mood <= 0) add("$at — mood должен быть > 0")
        // Игра учит откладывать понемногу, а не «никогда ничего»: копить всё подряд не должно выигрывать.
        val saveAll = TaskEngines.evaluate(task, TaskInput.DailyDeposits(List(task.days) { task.incomePerDay }))
        if ((saveAll as? TaskEvaluation.Done)?.outcome == TaskOutcome.SUCCESS) {
            add("$at — выигрывает «откладывать всё»: соблазнов меньше, чем сердечек в mood")
        } else if (task.startSaved + task.days * task.incomePerDay >= task.goal.price && TaskEngines.raceSolution(task) == null) {
            add("$at — цель не набрать, сохранив Финни настроение: не хватает свободных монет на радости")
        }
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

    private fun MutableList<String>.checkReceipt(task: ReceiptTask, at: String) {
        if (task.cart.isEmpty()) add("$at — cart пустой")
        if (task.lines.isEmpty()) add("$at — lines пустой")
        duplicates(task.cart.map { it.id }).forEach { add("$at — повторяется товар $it") }
        duplicates(task.lines.map { it.id }).forEach { add("$at — повторяется строка $it") }
        task.cart.filter { it.price <= 0 || it.qty <= 0 }.forEach { add("$at — ${it.id}: цена и qty должны быть > 0") }
        task.lines.filter { it.price <= 0 }.forEach { add("$at — строка ${it.id}: цена должна быть > 0") }
        val cart = task.cart.associateBy { it.id }
        task.lines.filter { it.item !in cart }.forEach { add("$at — строка ${it.id} ссылается на ${it.item}") }
        for (item in task.cart) {
            val lines = task.lines.filter { it.item == item.id }
            val prices = lines.map { it.price }.toSet()
            // Недопробитый товар — ошибка в пользу покупателя, её в игре не ищут.
            if (lines.size < item.qty) add("$at — ${item.id}: в чеке меньше строк, чем штук в пакете")
            if (prices.size > 1) add("$at — ${item.id}: у строк разная цена")
            val wrong = prices.singleOrNull()?.takeIf { it != item.price } ?: continue
            if (wrong < item.price) add("$at — ${item.id}: в чеке дешевле ценника, ошибка в пользу покупателя")
            if (lines.size > item.qty) add("$at — ${item.id}: и не та цена, и лишняя строка — выберите одну ошибку")
        }
        val total = TaskEngines.receiptTotal(task)
        if (task.paid < total) add("$at — paid меньше суммы чека $total")
        if (task.change < 0 || task.change > task.paid - total) {
            add("$at — change должен быть от 0 до ${task.paid - total}: больше — ошибка в пользу покупателя")
        }
        val errors = TaskEngines.receiptErrors(task)
        if (errors.lines.isEmpty() && errors.changeShort == 0) add("$at — в чеке нет ни одной ошибки: искать нечего")
    }

    /** Неделя до 7 дней, до 6 дел, до 3 часиков: ребёнку обозримо, решателю — мгновенно. */
    private fun MutableList<String>.checkChores(task: ChoresTask, at: String) {
        val before = size
        if (task.days !in 1..7) add("$at — days должен быть от 1 до 7")
        if (task.hoursPerDay !in 1..3) add("$at — hoursPerDay должен быть от 1 до 3")
        if (task.minRestDays < 1 || task.minRestDays >= task.days) add("$at — minRestDays должен быть от 1 и меньше days")
        if (task.goal.price <= 0) add("$at — goal.price должен быть > 0")
        if (task.chores.size !in 1..6) add("$at — дел должно быть от 1 до 6")
        duplicates(task.chores.map { it.id }).forEach { add("$at — повторяется дело $it") }
        task.chores.filter { it.hours !in 1..task.hoursPerDay }.forEach { add("$at — ${it.id}: hours от 1 до ${task.hoursPerDay}") }
        task.chores.filter { it.reward <= 0 }.forEach { add("$at — ${it.id}: reward должен быть > 0") }
        task.chores.filter { (it.maxTimes ?: 1) <= 0 }.forEach { add("$at — ${it.id}: maxTimes должен быть > 0") }
        // Перебор только по целым числам: с битыми он бессмыслен или долог.
        if (size == before && TaskEngines.choresSolution(task) == null) {
            add("$at — цель не набрать, даже если работать во все дни, кроме отдыха")
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

    private const val MAX_SHELF = 12

    private fun duplicates(ids: List<String>): Set<String> =
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
