package ru.finney.pet.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.GameStore
import ru.finney.pet.domain.game.ProfileResult
import ru.finney.pet.domain.game.Rejection
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.profile.PetNameError

class GameStoreTest {

    private val appearance = PetAppearance(PetCharacter.PUSHISTIK, BodyColor.B, EyesVariant.SLY)

    @Test
    fun `имя питомца обрезается и проверяется`() = runBlocking {
        val store = GameStore(Fixtures.game(), FakeStorage())

        assertEquals(ProfileResult.InvalidName(PetNameError.BLANK), store.createProfile("   ", appearance))
        assertEquals(ProfileResult.InvalidName(PetNameError.TOO_LONG), store.createProfile("Ф".repeat(17), appearance))

        val storage = FakeStorage()
        val id = (GameStore(Fixtures.game(), storage).createProfile("  Финни ", appearance) as ProfileResult.Saved).profileId
        assertEquals("Финни", storage.load(id)!!.profile.petName)
    }

    @Test
    fun `успешная команда сохраняется, отказ — нет`() = runBlocking {
        val storage = FakeStorage()
        val store = GameStore(Fixtures.game(), storage)
        val id = (store.createProfile("Финни", appearance) as ProfileResult.Saved).profileId
        val saves = storage.saves

        assertEquals(Rejection.PlanNotConfirmed, (store.execute(id) { buy(it, "apple") } as GameResult.Rejected).reason)
        assertEquals(saves, storage.saves)

        store.execute(id) { confirmPlan(it, needs = 20, wants = 0, savings = 0) }
        store.execute(id) { buy(it, "apple") }
        store.submitTask(id, "t1", Fixtures.success)

        assertEquals(saves + 3, storage.saves)
        assertEquals(55, storage.load(id)!!.state.balance)
    }

    @Test
    fun `параллельные покупки выполняются по очереди и не уводят баланс в минус`() = runBlocking {
        val storage = FakeStorage()
        val store = GameStore(Fixtures.game(), storage)
        val id = (store.createProfile("Финни", appearance) as ProfileResult.Saved).profileId
        store.execute(id) { confirmPlan(it, needs = 0, wants = 50, savings = 0) }

        val results = (1..30).map {
            async(Dispatchers.Default) { store.execute(id) { buy(it, "candy") } }
        }.awaitAll()

        assertEquals(10, results.count { it is GameResult.Ok })
        assertEquals(0, storage.load(id)!!.state.balance)
    }

    @Test
    fun `повторная настройка меняет внешность, но не прогресс`() = runBlocking {
        val storage = FakeStorage()
        val store = GameStore(Fixtures.game(), storage)
        val id = (store.createProfile("Финни", appearance) as ProfileResult.Saved).profileId
        store.execute(id) { confirmPlan(it, 20, 0, 0) }
        val before = storage.load(id)!!.state

        val newLook = PetAppearance(PetCharacter.PUSHISTIK, BodyColor.C, EyesVariant.ROUND)
        store.updateProfile(id, "Бублик", newLook)

        val after = storage.load(id)!!
        assertEquals(newLook, after.profile.appearance)
        assertEquals("Бублик", after.profile.petName)
        assertEquals(before, after.state)
    }

    @Test
    fun `сброс прогресса оставляет питомца и начинает игру заново`() = runBlocking {
        val storage = FakeStorage()
        val store = GameStore(Fixtures.game(), storage)
        val id = (store.createProfile("Финни", appearance, isDemo = true) as ProfileResult.Saved).profileId
        val start = storage.load(id)!!
        store.execute(id) { confirmPlan(it, needs = 20, wants = 0, savings = 0) }
        store.execute(id) { buy(it, "apple") }
        store.submitTask(id, "t1", Fixtures.success)
        store.execute(id) { closePeriod(it) }

        store.resetProgress(id)

        val reset = storage.load(id)!!
        assertEquals(start.profile, reset.profile)
        assertEquals(1, reset.state.periods.size)
        assertTrue(reset.state.attempts.isEmpty())
        assertEquals(start.state.balance, reset.state.balance)
        assertEquals(start.state.pet, reset.state.pet)
        assertTrue(reset.state.isDemo)
    }

    @Test
    fun `удаление профиля и всех данных`() = runBlocking {
        val storage = FakeStorage()
        val store = GameStore(Fixtures.game(), storage)
        val first = (store.createProfile("Финни", appearance, isDemo = true) as ProfileResult.Saved).profileId
        store.createProfile("Бублик", appearance)

        assertTrue(storage.load(first)!!.state.isDemo)
        store.deleteProfile(first)
        assertNull(storage.load(first))
        assertEquals(1, storage.all.value.size)

        store.deleteAll()
        assertTrue(storage.all.value.isEmpty())
    }
}
