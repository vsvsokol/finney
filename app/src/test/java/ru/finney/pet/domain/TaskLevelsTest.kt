package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.content.ContentValidator
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.game.TaskResult
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.model.GoalSliderTask
import ru.finney.pet.domain.model.Period
import ru.finney.pet.domain.model.PeriodFacts
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.PeriodResult
import ru.finney.pet.domain.model.SorterTask
import ru.finney.pet.domain.model.TaskTheme

/** Варианты одной игры по уровням: сложность растёт вместе с питомцем. */
class TaskLevelsTest {

    private fun variant(id: String, level: Int, theme: TaskTheme = TaskTheme.SAVINGS) = GoalSliderTask(
        id = id,
        theme = theme,
        title = "Копилка",
        intro = "",
        explainOk = "",
        explainFail = "",
        unlockLevel = level,
        series = "piggy",
        goalPrice = 30,
        periods = 3,
        incomePerPeriod = 50,
    )

    // В JSON варианты могут стоять в любом порядке — игра всё равно идёт от простого к сложному.
    private val content = Fixtures.content.copy(
        tasks = listOf(variant("easy", 1), variant("hard", 7), Fixtures.tasks.first(), variant("mid", 4)),
    )
    private val game = Game(content) { 0L }

    /** Очки берутся из итогов закрытых периодов — подкладываем один закрытый период с нужной суммой. */
    private fun GameState.atLevel(level: Int): GameState {
        val points = (level - 1) * Fixtures.economy.pointsPerLevel
        val closed = Period(
            number = 0,
            stage = 1,
            phase = PeriodPhase.CLOSED,
            result = PeriodResult(PeriodFacts(0, 0, 0, 0), false, false, false, 0, points),
        )
        return copy(periods = listOf(closed) + periods)
    }

    @Test
    fun `варианты собираются в одну игру от простого к сложному`() {
        assertEquals(listOf(listOf("easy", "mid", "hard"), listOf("t1")), content.taskSeries.map { v -> v.map { it.id } })
    }

    @Test
    fun `вариант игры выбирается по уровню питомца`() {
        val variants = content.taskSeries.first()
        val start = game.newGame()
        assertEquals("easy", game.currentVariant(start, variants).id)
        assertEquals("easy", game.currentVariant(start.atLevel(3), variants).id)
        assertEquals("mid", game.currentVariant(start.atLevel(4), variants).id)
        assertEquals("hard", game.currentVariant(start.atLevel(9), variants).id)
        assertEquals(listOf("mid", "t1"), game.currentTasks(start.atLevel(5)).map { it.id })
    }

    @Test
    fun `сложный вариант закрыт до своего уровня, в демо-режиме открыт`() {
        assertEquals(TaskResult.Rejected(Rejection.TaskLocked), game.submitTask(game.newGame(), "hard", Fixtures.success))
        game.submitTask(game.newGame().atLevel(7), "hard", Fixtures.success).submitted()
        game.submitTask(game.newGame(isDemo = true), "hard", Fixtures.success).submitted()
    }

    @Test
    fun `награда за каждый вариант своя`() {
        val first = game.submitTask(game.newGame().atLevel(7), "easy", Fixtures.success).submitted()
        val second = game.submitTask(first.state, "hard", Fixtures.success).submitted()
        assertEquals(Fixtures.economy.taskReward.success, second.reward)
    }

    @Test
    fun `итоги периода знают, какие игры стали сложнее`() {
        assertEquals(listOf("mid"), game.tasksUnlockedBetween(3, 4).map { it.id })
        assertEquals(listOf("hard", "mid"), game.tasksUnlockedBetween(1, 9).map { it.id })
        assertEquals(emptyList<String>(), game.tasksUnlockedBetween(4, 4).map { it.id })
    }

    @Test
    fun `проверка контента ловит несогласованные варианты`() {
        val sorter = SorterTask(
            id = "sorter",
            theme = TaskTheme.PLANNING,
            title = "",
            intro = "",
            explainOk = "",
            explainFail = "",
            series = "piggy",
            items = emptyList(),
            minCorrect = 0,
        )
        val broken = Fixtures.content.copy(
            tasks = listOf(variant("a", 1), variant("b", 1, TaskTheme.SHOPPING), variant("c", 12), sorter),
        )
        val errors = ContentValidator.validate(broken)
        listOf("разные движки", "разные темы", "два варианта с unlockLevel 1", "unlockLevel должен быть от 1 до 9").forEach { expected ->
            assertTrue("нет ошибки «$expected» в $errors", errors.any { expected in it })
        }
    }
}
