package ru.finney.pet.domain.tasks

import ru.finney.pet.domain.model.BasketRule
import ru.finney.pet.domain.model.BasketTask
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.ChangeMode
import ru.finney.pet.domain.model.ChangeRound
import ru.finney.pet.domain.model.ChangeTask
import ru.finney.pet.domain.model.Chore
import ru.finney.pet.domain.model.ChoresTask
import ru.finney.pet.domain.model.DistributorRule
import ru.finney.pet.domain.model.DistributorTask
import ru.finney.pet.domain.model.GoalRaceTask
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.RaceEvent
import ru.finney.pet.domain.model.ReceiptItem
import ru.finney.pet.domain.model.ReceiptLine
import ru.finney.pet.domain.model.ReceiptTask
import ru.finney.pet.domain.model.ReserveTask
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.StandTask
import ru.finney.pet.domain.model.TaskDefinition
import ru.finney.pet.domain.model.TaskOutcome

sealed interface TaskInput {
    /** distributor: сумма по id корзины. Отсутствующая корзина = 0. */
    data class Distribution(val amounts: Map<String, Int>) : TaskInput

    /** basket: выбранные товары с полки. */
    data class Basket(val items: Set<String>) : TaskInput

    /** goal_slider: регулярный взнос. */
    data class Deposit(val amount: Int) : TaskInput

    /** sorter: куда ребёнок отнёс каждую вещь — первый выбор, до подсказки. */
    data class Sorting(val answers: Map<String, Category>) : TaskInput

    /** goal_race: сколько отложено в каждый день, по порядку. */
    data class DailyDeposits(val amounts: List<Int>) : TaskInput

    /** reserve: что взято в план недели и что перенесено ради непредвиденной траты. */
    data class Reserve(val planned: Set<String>, val dropped: Set<String>) : TaskInput

    /** stand: сколько штук сырья закуплено. */
    data class Stock(val count: Int) : TaskInput

    /** change: монеты каждого раунда с первой попытки, по порядку раундов. */
    data class Coins(val rounds: List<List<Int>>) : TaskInput

    /** receipt: отмеченные строки чека и отмечена ли сдача — с первой проверки. */
    data class Flags(val lines: Set<String>, val change: Boolean) : TaskInput

    /** chores: id дел по дням недели. Пустой день — отдых. */
    data class Schedule(val days: List<List<String>>) : TaskInput
}

/** Ввод, который нельзя оценить. Это не исход задания: ребёнок исправляет и пробует снова. */
sealed interface TaskInputError {
    data object WrongInputType : TaskInputError
    data object NegativeAmount : TaskInputError
    data class UnknownBasket(val id: String) : TaskInputError
    data class UnknownItem(val id: String) : TaskInputError

    /** [remaining] > 0 — разложено не всё, < 0 — разложено больше суммы. */
    data class NotFullyDistributed(val remaining: Int) : TaskInputError

    /** Оплата сверх лимита — «не хватает [shortage] монет», как в магазине. */
    data class OverLimit(val total: Int, val limit: Int) : TaskInputError {
        val shortage: Int get() = total - limit
    }

    data class DepositOutOfRange(val max: Int) : TaskInputError
    data class DepositNotOnStep(val step: Int) : TaskInputError

    /** sorter: разложены не все вещи. */
    data class NotAllSorted(val missing: Int) : TaskInputError

    /** goal_race и change: дней или раундов не столько, сколько в задании. */
    data class WrongCount(val expected: Int) : TaskInputError

    /** reserve: нужное нельзя убрать из плана. */
    data class NeedsNotPlanned(val id: String) : TaskInputError

    /** reserve: нужное нельзя перенести ради непредвиденной траты. */
    data class NeedsLocked(val id: String) : TaskInputError

    /** reserve: перенести можно только то, что было в плане. */
    data class NotPlanned(val id: String) : TaskInputError

    /** reserve: непредвиденная трата всё ещё не покрыта — не хватает [shortage]. */
    data class SurpriseNotCovered(val shortage: Int) : TaskInputError

    /** change: такой монеты нет в ящике. */
    data class UnknownCoin(val value: Int) : TaskInputError

    /** change: в кошельке нет таких монет, раунд [round] с 0. */
    data class CoinsNotInWallet(val round: Int) : TaskInputError

    /** chores: в день [day] (с 0) дел больше, чем часиков. */
    data class DayOverloaded(val day: Int) : TaskInputError

    /** chores: дело [id] взято больше [max] раз за неделю. */
    data class ChoreTooOften(val id: String, val max: Int) : TaskInputError
}

/** Числа для объяснения исхода. */
sealed interface TaskDetails {
    /** Заполнено, если у задания есть `goal`. */
    data class Distribution(val goalSavedAfter: Int?, val goalRemaining: Int?) : TaskDetails
    data class Basket(val total: Int) : TaskDetails
    data class GoalSlider(val collected: Int, val shortfall: Int) : TaskDetails

    /** [mistakes] — id вещей, отнесённых не туда, в порядке задания. */
    data class Sorting(val correct: Int, val total: Int, val mistakes: List<String>) : TaskDetails

    /**
     * [eventsTaken] — сколько соблазнов дня ребёнок себе позволил,
     * [mood] — сердечки Финни на финише: 0 — загрустил по дороге.
     */
    data class GoalRace(val saved: Int, val shortfall: Int, val eventsTaken: Int, val mood: Int) : TaskDetails

    /** [reserve] — запас после плана; [shortage] — сколько не хватило запаса на непредвиденное. */
    data class Reserve(val reserve: Int, val shortage: Int) : TaskDetails

    /**
     * Итог дня лавки. [leftover] — непроданные стаканы, [missed] — гости, которым не хватило.
     * [best] — сколько сырья было бы в самый раз.
     */
    data class Stand(
        val cups: Int,
        val sold: Int,
        val earned: Int,
        val spent: Int,
        val leftover: Int,
        val missed: Int,
        val best: Int,
    ) : TaskDetails {
        val kept: Int get() = earned - spent
    }

    /** [results] — по раунду: положено минус нужно. 0 — верно, > 0 — лишнее, < 0 — не хватает. */
    data class Change(val correct: Int, val total: Int, val results: List<Int>) : TaskDetails

    /**
     * Сверка чека. [found] — ошибок отмечено верно, [missed] — не замечено, [extra] — верное
     * отмечено как ошибка. [refund] — сколько монет вернули за найденное, [lost] — за пропущенное.
     */
    data class Receipt(val found: Int, val missed: Int, val extra: Int, val refund: Int, val lost: Int) : TaskDetails

    /** [restDays] — пустых дней недели. */
    data class Chores(val earned: Int, val shortfall: Int, val restDays: Int) : TaskDetails
}

/** Ошибки кассира: строки чека, которые нужно отметить, и сколько сдачи недодали. */
data class ReceiptErrors(val lines: Set<String>, val changeShort: Int)

sealed interface TaskEvaluation {
    data class Done(val outcome: TaskOutcome, val details: TaskDetails) : TaskEvaluation
    data class Invalid(val error: TaskInputError) : TaskEvaluation
}

/** Движки заданий: учебные (docs/economy.md, раздел 9) и мини-игры (docs/minigames.md). */
object TaskEngines {

    fun evaluate(task: TaskDefinition, input: TaskInput): TaskEvaluation = when {
        task is DistributorTask && input is TaskInput.Distribution -> distribute(task, input)
        task is BasketTask && input is TaskInput.Basket -> checkout(task, input)
        task is GoalSliderTask && input is TaskInput.Deposit -> slide(task, input)
        task is SorterTask && input is TaskInput.Sorting -> sort(task, input)
        task is GoalRaceTask && input is TaskInput.DailyDeposits -> race(task, input)
        task is ReserveTask && input is TaskInput.Reserve -> reserve(task, input)
        task is StandTask && input is TaskInput.Stock -> stand(task, input)
        task is ChangeTask && input is TaskInput.Coins -> change(task, input)
        task is ReceiptTask && input is TaskInput.Flags -> receipt(task, input)
        task is ChoresTask && input is TaskInput.Schedule -> chores(task, input)
        else -> TaskEvaluation.Invalid(TaskInputError.WrongInputType)
    }

    // ---------- Подсказки для экранов: те же правила, что в оценке ----------

    /** Выполнено ли правило корзины для выбранных товаров — для галочек списка покупок. */
    fun ruleMet(task: BasketTask, rule: BasketRule, items: Set<String>): Boolean {
        val chosen = task.shelf.filter { it.id in items }
        return when (rule) {
            is BasketRule.HasCategory -> chosen.any { it.category == rule.category }
            is BasketRule.Includes -> rule.item in items
            is BasketRule.Excludes -> rule.item !in items
            is BasketRule.AnyOf -> rule.items.any { it in items }
            is BasketRule.MinQty -> chosen.filter { it.id in rule.items }.sumOf { it.qty } >= rule.qty
        }
    }

    /** Разница «положено минус нужно» для раунда кассы: 0 — верно. */
    fun changeDiff(round: ChangeRound, coins: List<Int>): Int = coins.sum() - round.target

    /** Можно ли заплатить этими монетами из кошелька раунда: каждой монеты — не больше, чем есть. */
    fun fitsWallet(round: ChangeRound, coins: List<Int>): Boolean {
        val have = round.wallet.groupingBy { it }.eachCount()
        return coins.groupingBy { it }.eachCount().all { (coin, n) -> n <= (have[coin] ?: 0) }
    }

    /** Итог лавки для [count] штук сырья — экран вечера показывает те же числа, что уйдут в оценку. */
    fun standDay(task: StandTask, count: Int): TaskDetails.Stand {
        val cups = count * task.ingredient.yields
        val sold = minOf(cups, task.guests)
        return TaskDetails.Stand(
            cups = cups,
            sold = sold,
            earned = sold * task.cupPrice,
            spent = count * task.ingredient.price,
            leftover = cups - sold,
            missed = task.guests - sold,
            best = bestStock(task),
        )
    }

    /** Сколько сырья хватит на всех гостей без лишнего: ⌈гости / стаканов из штуки⌉. */
    fun bestStock(task: StandTask): Int = (task.guests + task.ingredient.yields - 1) / task.ingredient.yields

    /** Позволил ли себе соблазн дня: после взноса на него хватает. */
    fun raceTaken(task: GoalRaceTask, event: RaceEvent, deposit: Int): Boolean = task.incomePerDay - deposit >= event.price

    /**
     * Сердечки Финни после каждого из [deposits] по порядку. Соблазн дня отвергнут —
     * минус одно, позволен — плюс одно, не больше [GoalRaceTask.mood]. Ноль — насовсем:
     * загрустившего Финни поздняя радость уже не выручает, важна регулярность.
     */
    fun raceMood(task: GoalRaceTask, deposits: List<Int>): List<Int> {
        var mood = task.mood
        return deposits.mapIndexed { i, deposit ->
            val event = task.events.firstOrNull { it.day == i + 1 }
            if (event != null && mood > 0) {
                mood = if (raceTaken(task, event, deposit)) minOf(task.mood, mood + 1) else mood - 1
            }
            mood
        }
    }

    /**
     * Взносы, с которыми дорога проходится, или null, если никак. Перебор того, какие
     * соблазны взять: в их день отложить остаток, в остальные — весь доход.
     */
    fun raceSolution(task: GoalRaceTask): List<Int>? {
        val events = task.events.filter { it.day in 1..task.days && it.price <= task.incomePerDay }
        return (0 until (1 shl events.size)).asSequence()
            .map { mask ->
                List(task.days) { i ->
                    val event = events.withIndex().firstOrNull { (n, e) -> e.day == i + 1 && mask and (1 shl n) != 0 }?.value
                    if (event == null) task.incomePerDay else (task.incomePerDay - event.price) / task.step * task.step
                }
            }
            .firstOrNull { (race(task, TaskInput.DailyDeposits(it)) as? TaskEvaluation.Done)?.outcome == TaskOutcome.SUCCESS }
    }

    /** Сумма по чеку — как её посчитал кассир, с ошибками. */
    fun receiptTotal(task: ReceiptTask): Int = task.lines.sumOf { it.price }

    /**
     * Что должно быть отмечено. Одинаковые строки одного товара взаимозаменяемы: из дубля
     * ошибкой названы сначала уже отмеченные в [flagged], потом последние — так экран
     * подсвечивает то же, что засчитывает оценка.
     */
    fun receiptErrors(task: ReceiptTask, flagged: Set<String> = emptySet()): ReceiptErrors = ReceiptErrors(
        lines = task.cart.flatMap { item ->
            val check = checkItem(task, item)
            val (marked, rest) = check.samePrice.partition { it.id in flagged }
            val duplicates = (marked + rest.reversed()).take(check.duplicates)
            check.wrongPrice.map { it.id } + duplicates.map { it.id }
        }.toSet(),
        changeShort = changeShort(task),
    )

    /** Недоданная сдача: сдача считается от суммы чека, какой бы она ни была. */
    private fun changeShort(task: ReceiptTask): Int = maxOf(0, task.paid - receiptTotal(task) - task.change)

    /**
     * Строки товара. Цена не как на ценнике — ошибка в каждой такой строке. Строк больше, чем
     * штук в пакете, — лишние [duplicates]. Оба вида ошибки у одного товара валидатор не пускает.
     */
    private class ItemCheck(val wrongPrice: List<ReceiptLine>, val samePrice: List<ReceiptLine>, val duplicates: Int)

    private fun checkItem(task: ReceiptTask, item: ReceiptItem): ItemCheck {
        val (wrong, same) = task.lines.filter { it.item == item.id }.partition { it.price != item.price }
        val duplicates = if (wrong.isEmpty()) maxOf(0, same.size - item.qty) else 0
        return ItemCheck(wrong, same, duplicates)
    }

    /** Сколько часиков занимают дела одного дня. Неизвестные id не считаются. */
    fun dayHours(task: ChoresTask, ids: List<String>): Int {
        val byId = task.chores.associateBy { it.id }
        return ids.sumOf { byId[it]?.hours ?: 0 }
    }

    /** Сколько заработано за неделю. */
    fun choresEarned(task: ChoresTask, days: List<List<String>>): Int {
        val byId = task.chores.associateBy { it.id }
        return days.flatten().sumOf { byId[it]?.reward ?: 0 }
    }

    /**
     * Неделя с самым большим заработком: сначала рабочие дни, в конце [ChoresTask.minRestDays]
     * отдыха. null — даже так цель не набрать. Перебор наборов дел на день; состояние —
     * сколько раз уже взяты дела с [Chore.maxTimes].
     */
    fun choresSolution(task: ChoresTask): List<List<String>>? {
        val workDays = task.days - task.minRestDays
        if (workDays < 0) return null
        val limited = task.chores.filter { it.maxTimes != null }
        val packs = dayPacks(task.chores, task.hoursPerDay)
        var best = mapOf(List(limited.size) { 0 } to (0 to emptyList<List<String>>()))
        repeat(workDays) {
            val next = mutableMapOf<List<Int>, Pair<Int, List<List<String>>>>()
            for ((used, value) in best) for (pack in packs) {
                val after = limited.mapIndexed { i, chore -> used[i] + pack.count { it == chore } }
                if (limited.indices.any { after[it] > limited[it].maxTimes!! }) continue
                val earned = value.first + pack.sumOf { it.reward }
                if (earned > (next[after]?.first ?: -1)) next[after] = earned to value.second + listOf(pack.map { it.id })
            }
            best = next
        }
        val (earned, week) = best.values.maxByOrNull { it.first } ?: return null
        if (earned < task.goal.price) return null
        return week + List(task.minRestDays) { emptyList() }
    }

    /** Все наборы дел, которые помещаются в один день, включая пустой. */
    private fun dayPacks(chores: List<Chore>, hours: Int): List<List<Chore>> {
        fun from(i: Int, left: Int): List<List<Chore>> {
            if (i == chores.size) return listOf(emptyList())
            val chore = chores[i]
            val most = minOf(left / chore.hours, chore.maxTimes ?: Int.MAX_VALUE)
            return (0..most).flatMap { n -> from(i + 1, left - n * chore.hours).map { List(n) { chore } + it } }
        }
        return from(0, hours)
    }

    /** Запас после плана: сумма минус всё запланированное. */
    fun reserveLeft(task: ReserveTask, planned: Set<String>): Int =
        task.amount - task.spendings.filter { it.id in planned }.sumOf { it.price }

    private fun distribute(task: DistributorTask, input: TaskInput.Distribution): TaskEvaluation {
        val basketIds = task.baskets.map { it.id }.toSet()
        input.amounts.keys.firstOrNull { it !in basketIds }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownBasket(it))
        }
        if (input.amounts.values.any { it < 0 }) return TaskEvaluation.Invalid(TaskInputError.NegativeAmount)
        val remaining = task.amount - input.amounts.values.sum()
        if (remaining != 0) return TaskEvaluation.Invalid(TaskInputError.NotFullyDistributed(remaining))

        fun amountIn(basket: String) = input.amounts[basket] ?: 0
        val success = task.rules.all { rule ->
            when (rule) {
                is DistributorRule.Min -> amountIn(rule.basket) >= rule.value
                is DistributorRule.Max -> amountIn(rule.basket) <= rule.value
            }
        }
        val savedAfter = task.goal?.let { it.saved + amountIn(it.basket) }
        val details = TaskDetails.Distribution(
            goalSavedAfter = savedAfter,
            goalRemaining = task.goal?.let { maxOf(0, it.price - savedAfter!!) },
        )
        return TaskEvaluation.Done(outcome(success), details)
    }

    private fun checkout(task: BasketTask, input: TaskInput.Basket): TaskEvaluation {
        val shelf = task.shelf.associateBy { it.id }
        input.items.firstOrNull { it !in shelf }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        val chosen = input.items.map { shelf.getValue(it) }
        val total = chosen.sumOf { it.price }
        if (total > task.limit) return TaskEvaluation.Invalid(TaskInputError.OverLimit(total, task.limit))

        val success = task.rules.all { ruleMet(task, it, input.items) }
        return TaskEvaluation.Done(outcome(success), TaskDetails.Basket(total))
    }

    private fun slide(task: GoalSliderTask, input: TaskInput.Deposit): TaskEvaluation {
        if (input.amount !in 0..task.incomePerPeriod) {
            return TaskEvaluation.Invalid(TaskInputError.DepositOutOfRange(task.incomePerPeriod))
        }
        if (input.amount % task.step != 0) return TaskEvaluation.Invalid(TaskInputError.DepositNotOnStep(task.step))
        val collected = input.amount * task.periods
        val details = TaskDetails.GoalSlider(collected = collected, shortfall = maxOf(0, task.goalPrice - collected))
        return TaskEvaluation.Done(outcome(collected >= task.goalPrice), details)
    }

    private fun sort(task: SorterTask, input: TaskInput.Sorting): TaskEvaluation {
        val ids = task.items.map { it.id }.toSet()
        input.answers.keys.firstOrNull { it !in ids }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        val missing = ids.count { it !in input.answers }
        if (missing > 0) return TaskEvaluation.Invalid(TaskInputError.NotAllSorted(missing))
        val mistakes = task.items.filter { input.answers[it.id] != it.category }.map { it.id }
        val correct = task.items.size - mistakes.size
        return TaskEvaluation.Done(
            outcome(correct >= task.minCorrect),
            TaskDetails.Sorting(correct = correct, total = task.items.size, mistakes = mistakes),
        )
    }

    private fun race(task: GoalRaceTask, input: TaskInput.DailyDeposits): TaskEvaluation {
        if (input.amounts.size != task.days) return TaskEvaluation.Invalid(TaskInputError.WrongCount(task.days))
        if (input.amounts.any { it !in 0..task.incomePerDay }) {
            return TaskEvaluation.Invalid(TaskInputError.DepositOutOfRange(task.incomePerDay))
        }
        if (input.amounts.any { it % task.step != 0 }) return TaskEvaluation.Invalid(TaskInputError.DepositNotOnStep(task.step))
        val saved = task.startSaved + input.amounts.sum()
        val taken = task.events.count { event ->
            val deposit = input.amounts.getOrNull(event.day - 1) ?: return@count false
            raceTaken(task, event, deposit)
        }
        // Цель — не «никогда ничего не тратить»: без радостей Финни грустит, и копилка не засчитывается.
        val mood = raceMood(task, input.amounts).last()
        return TaskEvaluation.Done(
            outcome(saved >= task.goal.price && mood > 0),
            TaskDetails.GoalRace(saved = saved, shortfall = maxOf(0, task.goal.price - saved), eventsTaken = taken, mood = mood),
        )
    }

    private fun reserve(task: ReserveTask, input: TaskInput.Reserve): TaskEvaluation {
        val byId = task.spendings.associateBy { it.id }
        (input.planned + input.dropped).firstOrNull { it !in byId }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        task.spendings.firstOrNull { it.category == Category.NEEDS && it.id !in input.planned }?.let {
            return TaskEvaluation.Invalid(TaskInputError.NeedsNotPlanned(it.id))
        }
        val reserve = reserveLeft(task, input.planned)
        if (reserve < 0) return TaskEvaluation.Invalid(TaskInputError.OverLimit(task.amount - reserve, task.amount))
        input.dropped.firstOrNull { it !in input.planned }?.let {
            return TaskEvaluation.Invalid(TaskInputError.NotPlanned(it))
        }
        input.dropped.firstOrNull { byId.getValue(it).category == Category.NEEDS }?.let {
            return TaskEvaluation.Invalid(TaskInputError.NeedsLocked(it))
        }

        val shortage = maxOf(0, task.surprise.price - reserve)
        val freed = input.dropped.sumOf { byId.getValue(it).price }
        if (freed < shortage) return TaskEvaluation.Invalid(TaskInputError.SurpriseNotCovered(shortage - freed))
        return TaskEvaluation.Done(outcome(shortage == 0), TaskDetails.Reserve(reserve = reserve, shortage = shortage))
    }

    private fun stand(task: StandTask, input: TaskInput.Stock): TaskEvaluation {
        if (input.count < 0) return TaskEvaluation.Invalid(TaskInputError.NegativeAmount)
        val cost = input.count * task.ingredient.price
        if (cost > task.budget) return TaskEvaluation.Invalid(TaskInputError.OverLimit(cost, task.budget))
        val day = standDay(task, input.count)
        return TaskEvaluation.Done(outcome(input.count == day.best), day)
    }

    private fun change(task: ChangeTask, input: TaskInput.Coins): TaskEvaluation {
        if (input.rounds.size != task.rounds.size) return TaskEvaluation.Invalid(TaskInputError.WrongCount(task.rounds.size))
        input.rounds.flatten().firstOrNull { it !in task.coins }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownCoin(it))
        }
        task.rounds.forEachIndexed { i, round ->
            if (round.mode == ChangeMode.EXACT && !fitsWallet(round, input.rounds[i])) {
                return TaskEvaluation.Invalid(TaskInputError.CoinsNotInWallet(i))
            }
        }
        val results = task.rounds.mapIndexed { i, round -> changeDiff(round, input.rounds[i]) }
        val correct = results.count { it == 0 }
        return TaskEvaluation.Done(
            outcome(correct >= (task.minCorrect ?: task.rounds.size)),
            TaskDetails.Change(correct = correct, total = task.rounds.size, results = results),
        )
    }

    private fun receipt(task: ReceiptTask, input: TaskInput.Flags): TaskEvaluation {
        val lineIds = task.lines.map { it.id }.toSet()
        input.lines.firstOrNull { it !in lineIds }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        var found = 0
        var missed = 0
        var extra = 0
        var refund = 0
        var lost = 0
        for (item in task.cart) {
            val check = checkItem(task, item)
            check.wrongPrice.forEach { line ->
                val over = line.price - item.price
                if (line.id in input.lines) { found++; refund += over } else { missed++; lost += over }
            }
            val flagged = check.samePrice.count { it.id in input.lines }
            val caught = minOf(flagged, check.duplicates)
            found += caught
            missed += check.duplicates - caught
            extra += flagged - caught
            refund += caught * item.price
            lost += (check.duplicates - caught) * item.price
        }
        val short = changeShort(task)
        when {
            short > 0 && input.change -> { found++; refund += short }
            short > 0 -> { missed++; lost += short }
            input.change -> extra++
        }
        return TaskEvaluation.Done(
            outcome(missed == 0 && extra == 0),
            TaskDetails.Receipt(found = found, missed = missed, extra = extra, refund = refund, lost = lost),
        )
    }

    private fun chores(task: ChoresTask, input: TaskInput.Schedule): TaskEvaluation {
        if (input.days.size != task.days) return TaskEvaluation.Invalid(TaskInputError.WrongCount(task.days))
        val byId = task.chores.associateBy { it.id }
        input.days.flatten().firstOrNull { it !in byId }?.let {
            return TaskEvaluation.Invalid(TaskInputError.UnknownItem(it))
        }
        input.days.indexOfFirst { dayHours(task, it) > task.hoursPerDay }.takeIf { it >= 0 }?.let {
            return TaskEvaluation.Invalid(TaskInputError.DayOverloaded(it))
        }
        val times = input.days.flatten().groupingBy { it }.eachCount()
        task.chores.firstOrNull { chore -> chore.maxTimes != null && (times[chore.id] ?: 0) > chore.maxTimes }?.let {
            return TaskEvaluation.Invalid(TaskInputError.ChoreTooOften(it.id, it.maxTimes!!))
        }
        val earned = choresEarned(task, input.days)
        val rest = input.days.count { it.isEmpty() }
        return TaskEvaluation.Done(
            outcome(earned >= task.goal.price && rest >= task.minRestDays),
            TaskDetails.Chores(earned = earned, shortfall = maxOf(0, task.goal.price - earned), restDays = rest),
        )
    }

    private fun outcome(success: Boolean) = if (success) TaskOutcome.SUCCESS else TaskOutcome.FAIL
}
