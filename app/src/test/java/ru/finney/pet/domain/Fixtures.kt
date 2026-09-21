package ru.finney.pet.domain

import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.TaskResult
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.EconomyConfig
import ru.finney.pet.domain.model.GameContent
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.Goal
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.ItemKind
import ru.finney.pet.domain.model.ParentBonusRule
import ru.finney.pet.domain.model.PetRule
import ru.finney.pet.domain.model.PetStats
import ru.finney.pet.domain.model.PointsRule
import ru.finney.pet.domain.model.ShopItem
import ru.finney.pet.domain.model.StatEffect
import ru.finney.pet.domain.model.TaskReward
import ru.finney.pet.domain.model.TaskTheme
import ru.finney.pet.domain.tasks.TaskInput

/**
 * Контент для тестов правил. Не зависит от assets: @vsvsokol меняет числа в JSON,
 * а тесты правил от этого не краснеют. Реальный контент проверяет ContentTest.
 */
object Fixtures {

    val economy = EconomyConfig(
        incomeByStage = listOf(50, 60, 70),
        taskReward = TaskReward(success = 15, fail = 5),
        parentBonus = ParentBonusRule(step = 5, maxPerPeriod = 20),
        planTolerance = 5,
        points = PointsRule(needsCovered = 2, planMatched = 2, savingsAdded = 2, taskSuccess = 1, taskSuccessMaxPerPeriod = 2),
        pointsPerLevel = 5,
        maxLevel = 9,
        stageStartLevels = listOf(1, 4, 7),
        pet = PetRule(
            start = PetStats(70, 70, 70),
            decayByStage = listOf(StatEffect(40, 30, 20), StatEffect(50, 35, 25), StatEffect(60, 40, 30)),
            needsThreshold = 50,
            emotionLow = 30,
            emotionHappy = 60,
        ),
    )

    val shop = listOf(
        ShopItem("apple", "Яблоко", 10, Category.NEEDS, effect = StatEffect(satiety = 20)),
        ShopItem("bowl", "Миска", 20, Category.NEEDS, effect = StatEffect(satiety = 45)),
        ShopItem("soap", "Мыло", 10, Category.NEEDS, effect = StatEffect(hygiene = 35)),
        ShopItem("towel", "Полотенце", 15, Category.NEEDS, effect = StatEffect(hygiene = 50)),
        ShopItem("candy", "Конфета", 5, Category.WANTS, effect = StatEffect(mood = 10)),
        ShopItem("ball", "Мячик", 15, Category.WANTS, effect = StatEffect(mood = 30)),
        ShopItem("lamp", "Лампа", 25, Category.WANTS, effect = StatEffect(mood = 50)),
        ShopItem("hat", "Шапка", 30, Category.WANTS, ItemKind.ACCESSORY, StatEffect(mood = 20)),
    )

    val goals = listOf(
        Goal("crown", "Корона", 50, moodBonus = 20),
        Goal("bike", "Велосипед", 100, moodBonus = 30),
    )

    /** t1..t10 — одинаковые простые задания: взнос 10 — успех, 5 — неудача. t10 открывается с 3-го периода. */
    val tasks = (1..10).map { n ->
        GoalSliderTask(
            id = "t$n",
            theme = TaskTheme.SAVINGS,
            title = "Задание $n",
            intro = "",
            explainOk = "",
            explainFail = "",
            unlockPeriod = if (n == 10) 3 else 1,
            goalPrice = 30,
            periods = 3,
            incomePerPeriod = 50,
        )
    }

    val content = GameContent(economy, shop, goals, tasks)

    val success = TaskInput.Deposit(10)
    val failure = TaskInput.Deposit(5)

    fun game(): Game {
        var now = 0L
        return Game(content) { ++now }
    }
}

fun GameResult.state(): GameState = when (this) {
    is GameResult.Ok -> state
    is GameResult.Rejected -> throw AssertionError("Ожидался успех, получен отказ: $reason")
}

fun GameResult.reason(): Rejection = when (this) {
    is GameResult.Ok -> throw AssertionError("Ожидался отказ, команда выполнена")
    is GameResult.Rejected -> reason
}

fun TaskResult.submitted(): TaskResult.Submitted = when (this) {
    is TaskResult.Submitted -> this
    is TaskResult.Rejected -> throw AssertionError("Ожидался исход задания, получен отказ: $reason")
}

/** Докупить нужное до порога самым простым способом. */
fun Game.coverNeeds(start: GameState): GameState {
    var s = start
    val threshold = Fixtures.economy.pet.needsThreshold
    while (s.pet.satiety < threshold) s = buy(s, if (threshold - s.pet.satiety <= 20) "apple" else "bowl").state()
    while (s.pet.hygiene < threshold) s = buy(s, if (threshold - s.pet.hygiene <= 35) "soap" else "towel").state()
    return s
}

/** Период с максимумом очков: нужное, план, копилка, два новых успешных задания. */
fun Game.playPerfectPeriod(start: GameState): GameState {
    var s = start
    if (s.activeGoalId == null) s = selectGoal(s, "bike").state()
    s = confirmPlan(s, needs = needsHint(s)!!, wants = 0, savings = 10).state()
    val done = s.attempts.map { it.taskId }.toSet()
    Fixtures.tasks.map { it.id }.filter { it !in done && isTaskAvailable(s, content(it)) }.take(2).forEach {
        s = submitTask(s, it, Fixtures.success).submitted().state
    }
    s = coverNeeds(s)
    // Отдельного пополнения нет: копилка в плане списывается при подтверждении.
    return closePeriod(s).state()
}

private fun content(id: String) = Fixtures.content.task(id)!!
