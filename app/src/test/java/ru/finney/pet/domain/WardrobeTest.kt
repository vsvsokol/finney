package ru.finney.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.Rejection

/** Гардероб: купленный аксессуар надевается сразу, переодевание бесплатное, радость растёт за покупку. */
class WardrobeTest {

    private val game = Fixtures.game()

    private fun active() = game.confirmPlan(game.newGame(), needs = 0, wants = 50, savings = 0).state()

    @Test
    fun `купленная шапка сразу надета, радость выросла, деньги списаны`() {
        val before = active()
        val after = game.buy(before, "hat").state()

        assertEquals("hat", after.wornItemId)
        assertEquals(before.balance - 30, after.balance)
        assertTrue("за покупку вещи питомец радуется", after.pet.mood > before.pet.mood)
        assertEquals(listOf("hat"), game.wardrobe(after).map { it.id })
    }

    @Test
    fun `вторую такую же шапку не купить`() {
        val withHat = game.buy(active(), "hat").state()
        assertEquals(Rejection.AlreadyOwned, game.buy(withHat, "hat").reason())
    }

    @Test
    fun `снять и надеть снова — бесплатно`() {
        val withHat = game.buy(active(), "hat").state()

        val bare = game.takeOff(withHat).state()
        assertNull(bare.wornItemId)
        assertEquals(withHat.balance, bare.balance)
        assertEquals(withHat.pet, bare.pet)

        val again = game.wear(bare, "hat").state()
        assertEquals("hat", again.wornItemId)
        assertEquals(withHat.balance, again.balance)
    }

    @Test
    fun `надеть можно только купленный аксессуар`() {
        val state = active()
        assertEquals(Rejection.NotOwned("hat"), game.wear(state, "hat").reason())
        assertEquals(Rejection.NotWearable("apple"), game.wear(state, "apple").reason())
        assertEquals(Rejection.UnknownItem("nope"), game.wear(state, "nope").reason())
    }

    @Test
    fun `обычная покупка не снимает шапку`() {
        val withHat = game.buy(active(), "hat").state()
        val fed = game.buy(withHat, "candy").state()
        assertEquals("hat", fed.wornItemId)
    }
}
