package ru.finney.pet.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finney.pet.domain.game.GameStore
import ru.finney.pet.domain.game.ProfileResult
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetAppearance

class SessionTest {

    private val appearance = PetAppearance(BodyColor.A, EyesVariant.OVAL)
    private val storage = FakeStorage()
    private val store = GameStore(Fixtures.game(), storage)

    @Test
    fun `первый запуск — профиля нет`() = runBlocking {
        val active = FakeActiveProfile()
        assertFalse(Session(store, active).restore())
        assertNull(active.activeProfileId.value)
    }

    @Test
    fun `созданный профиль сразу открыт, команды идут в него`() = runBlocking {
        val session = Session(store, FakeActiveProfile())
        val id = (session.createProfile("Финни", appearance) as ProfileResult.Saved).profileId

        session.execute { confirmPlan(it, needs = 20, wants = 0, savings = 0) }
        session.execute { buy(it, "apple") }

        assertEquals(40, storage.load(id)!!.state.balance)
        assertEquals(40, session.activeGame.first()!!.state.balance)
        assertTrue(session.restore())
    }

    @Test
    fun `устаревший id — открывается оставшийся профиль`() = runBlocking {
        val id = (store.createProfile("Финни", appearance) as ProfileResult.Saved).profileId
        val active = FakeActiveProfile(initial = 42)

        assertTrue(Session(store, active).restore())
        assertEquals(id, active.activeProfileId.value)
    }

    @Test
    fun `удаление открытого профиля переключает на другой, затем на первый запуск`() = runBlocking {
        val active = FakeActiveProfile()
        val session = Session(store, active)
        val first = (session.createProfile("Финни", appearance) as ProfileResult.Saved).profileId
        val demo = (session.createProfile("Демо", appearance, isDemo = true) as ProfileResult.Saved).profileId
        assertEquals(demo, active.activeProfileId.value)

        session.deleteActiveProfile()
        assertEquals(first, active.activeProfileId.value)
        assertEquals("Финни", session.activeGame.first()!!.profile.petName)

        session.deleteActiveProfile()
        assertNull(active.activeProfileId.value)
        assertNull(session.activeGame.first())
        assertFalse(session.restore())
    }

    @Test
    fun `удаление всех данных возвращает к первому запуску`() = runBlocking {
        val active = FakeActiveProfile()
        val session = Session(store, active)
        session.createProfile("Финни", appearance)

        session.deleteAll()

        assertNull(active.activeProfileId.value)
        assertTrue(storage.all.value.isEmpty())
    }
}
