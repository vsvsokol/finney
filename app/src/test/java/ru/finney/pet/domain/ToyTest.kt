package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.GameState
import ru.finney.pet.domain.pet.PetRules

/**
 * Игрушки: покупаются один раз и лежат в зале, с ними играют — настроение растёт,
 * но за одну сессию игры не больше sessionMoodCap. Сессия — час (в демо — 30 с) или до нового уровня.
 */
class ToyTest {

    private var now = 0L
    private val game = Game(Fixtures.content) { now }
    private val rule = Fixtures.economy.play
    private val hour = 60 * 60_000L

    /** Купил мишку; настроение опущено, чтобы прибавке было куда расти. */
    private fun withTeddy(isDemo: Boolean = false): GameState {
        val planned = game.confirmPlan(game.newGame(isDemo), 0, 20, 0).state()
        val bought = game.buy(planned, "teddy").state()
        return bought.copy(pet = bought.pet.copy(mood = 10))
    }

    @Test
    fun `игрушка покупается за деньги и остаётся в зале`() {
        val planned = game.confirmPlan(game.newGame(), 0, 20, 0).state()
        val bought = game.buy(planned, "teddy").state()

        assertEquals(planned.balance - 20, bought.balance)
        assertEquals(planned.pet.mood + 10, bought.pet.mood)
        assertEquals(listOf("teddy"), game.toys(bought).map { it.id })
        // Игрушку не надевают.
        assertNull(bought.wornItemId)
    }

    @Test
    fun `вторую такую же игрушку не купить`() {
        val bought = withTeddy()
        assertEquals(Rejection.AlreadyOwned, game.buy(bought, "teddy").reason())
    }

    @Test
    fun `игрушка остаётся после конца уровня`() {
        val next = game.closePeriod(withTeddy()).state()
        assertEquals(listOf("teddy"), game.toys(next).map { it.id })
    }

    @Test
    fun `потряс игрушкой — настроение растёт на moodPerShake за раз`() {
        val played = game.play(withTeddy(), "teddy", shakes = 3).state()
        assertEquals(10 + 3 * rule.moodPerShake, played.pet.mood)
        assertEquals(rule.sessionMoodCap - 3 * rule.moodPerShake, game.playMoodLeft(played))
    }

    @Test
    fun `за сессию не больше sessionMoodCap — дальше игрушка не радует`() {
        var s = withTeddy()
        repeat(50) { s = game.play(s, "teddy", shakes = 1).state() }

        assertEquals(10 + rule.sessionMoodCap, s.pet.mood)
        assertEquals(0, game.playMoodLeft(s))
        // Наигрался: команда проходит, но ничего не меняет.
        assertEquals(s, game.play(s, "teddy", shakes = 5).state())
    }

    @Test
    fun `через час сессия новая — игрушка снова радует`() {
        var s = withTeddy()
        s = game.play(s, "teddy", shakes = 100).state()
        assertEquals(0, game.playMoodLeft(s))

        now += hour - 1
        assertEquals(0, game.playMoodLeft(s))
        now += 1
        assertEquals(rule.sessionMoodCap, game.playMoodLeft(s))
        val again = game.play(s, "teddy", shakes = 1).state()
        assertEquals(s.pet.mood + rule.moodPerShake, again.pet.mood)
        assertEquals(now, again.playSince)
    }

    @Test
    fun `новый уровень — новая сессия игры`() {
        val tired = game.play(withTeddy(), "teddy", shakes = 100).state()
        assertEquals(0, game.playMoodLeft(tired))

        val next = game.closePeriod(tired).state()
        assertEquals(rule.sessionMoodCap, game.playMoodLeft(next))
        assertEquals(0, next.playMood)
        assertNull(next.playSince)
    }

    @Test
    fun `в демо сессия игры — секунды`() {
        val s = game.play(withTeddy(isDemo = true), "teddy", shakes = 100).state()
        now += rule.demoSessionSeconds * 1_000L
        assertEquals(rule.sessionMoodCap, game.playMoodLeft(s))
    }

    @Test
    fun `настроение не выше 100 и в сессию идёт только настоящая прибавка`() {
        val full = withTeddy().let { it.copy(pet = it.pet.copy(mood = PetRules.STAT_MAX - 3)) }
        val played = game.play(full, "teddy", shakes = 10).state()
        assertEquals(PetRules.STAT_MAX, played.pet.mood)
        assertEquals(3, played.playMood)
        // На сотне прибавки нет — и сессия не начинается.
        val top = full.copy(pet = full.pet.copy(mood = PetRules.STAT_MAX))
        assertNull(game.play(top, "teddy", shakes = 1).state().playSince)
    }

    @Test
    fun `во сне игрушки не работают`() {
        val tired = withTeddy().let { it.copy(pet = it.pet.copy(energy = 20)) }
        val asleep = game.sleep(tired).state()
        assertEquals(Rejection.Asleep, game.play(asleep, "teddy", shakes = 1).reason())
    }

    @Test
    fun `играть можно только с купленной игрушкой`() {
        val planned = game.confirmPlan(game.newGame(), 0, 20, 0).state()
        assertEquals(Rejection.NotOwned("teddy"), game.play(planned, "teddy", 1).reason())
        assertEquals(Rejection.NotPlayable("candy"), game.play(planned, "candy", 1).reason())
        assertEquals(Rejection.InvalidAmount, game.play(withTeddy(), "teddy", 0).reason())
    }

    @Test
    fun `играть можно и до плана — игрушка уже куплена`() {
        val bought = withTeddy()
        val nextPlanning = game.closePeriod(bought).state().let { it.copy(pet = it.pet.copy(mood = 10)) }
        val played = game.play(nextPlanning, "teddy", 1).state()
        assertEquals(10 + rule.moodPerShake, played.pet.mood)
    }
}
