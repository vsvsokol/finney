package ru.finney.pet.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.finney.pet.content.AssetContentLoader
import ru.finney.pet.data.db.FinneyDatabase
import ru.finney.pet.data.repository.RoomGameStorage
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.GameResult
import ru.finney.pet.domain.game.GameStore
import ru.finney.pet.domain.game.ProfileResult
import ru.finney.pet.domain.game.TaskResult
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetAppearance
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.tasks.TaskInput

/** ТЗ п. 2.5.13: профиль, баланс, покупки, накопления, цель и прогресс переживают перезапуск. */
@RunWith(AndroidJUnit4::class)
class RoomGameStorageTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "room-storage-test.db"
    private val game = Game(AssetContentLoader(context).load())
    private lateinit var db: FinneyDatabase

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        db = open()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
    }

    private fun open() = Room.databaseBuilder(context, FinneyDatabase::class.java, dbName).build()

    private fun store(database: FinneyDatabase) = GameStore(game, RoomGameStorage(database.gameDao()))

    private suspend fun GameStore.ok(id: Long, command: Game.(ru.finney.pet.domain.model.GameState) -> GameResult) {
        val result = execute(id, command)
        assertTrue("команда отклонена: $result", result is GameResult.Ok)
    }

    /** Два периода со всеми видами операций. */
    private suspend fun playTwoPeriods(store: GameStore, id: Long) {
        repeat(2) { round ->
            store.ok(id) { selectGoal(it, "goal_bike") }
            store.ok(id) { addParentBonus(it, 10) }
            store.ok(id) { confirmPlan(it, needs = 30, wants = 10, savings = 10) }
            store.ok(id) { buy(it, "food_bowl") }
            store.ok(id) { buy(it, "care_soap") }
            store.ok(id) { buy(it, "treat_candy") }
            val task = if (round == 0) "savings_01" else "savings_02"
            val input = if (round == 0) TaskInput.Deposit(30) else TaskInput.Distribution(mapOf("wants" to 40))
            assertTrue(store.submitTask(id, task, input) is TaskResult.Submitted)
            store.ok(id) { deposit(it, 15) }
            store.ok(id) { withdraw(it, 5) }
            store.ok(id) { closePeriod(it) }
        }
    }

    @Test
    fun stateSurvivesDatabaseReopen() = runBlocking {
        val store = store(db)
        val look = PetAppearance(PetCharacter.PUSHISTIK, BodyColor.C, EyesVariant.OVAL)
        val id = (store.createProfile("Финни", look, isDemo = true) as ProfileResult.Saved).profileId
        playTwoPeriods(store, id)
        val before = RoomGameStorage(db.gameDao()).load(id)!!

        db.close()
        db = open()
        val after = RoomGameStorage(db.gameDao()).load(id)!!

        assertEquals(before, after)
        assertEquals(3, after.state.periods.size)
        assertTrue(after.state.periods.take(2).all { it.result != null })
        assertTrue(after.state.balance >= 0)
        assertEquals(20, after.state.goalSaved("goal_bike"))
        assertEquals("Финни", after.profile.petName)
        assertEquals(look, after.profile.appearance)
    }

    @Test
    fun savedStateEqualsInMemoryGame() = runBlocking {
        val store = store(db)
        val id = (store.createProfile("Финни", PetAppearance(PetCharacter.PUSHISTIK, BodyColor.A, EyesVariant.ROUND)) as ProfileResult.Saved).profileId
        playTwoPeriods(store, id)

        // Тот же сценарий без базы: сохранение не должно ничего терять или добавлять.
        var expected = game.newGame()
        fun apply(result: GameResult) {
            expected = (result as GameResult.Ok).state
        }
        repeat(2) { round ->
            apply(game.selectGoal(expected, "goal_bike"))
            apply(game.addParentBonus(expected, 10))
            apply(game.confirmPlan(expected, 30, 10, 10))
            apply(game.buy(expected, "food_bowl"))
            apply(game.buy(expected, "care_soap"))
            apply(game.buy(expected, "treat_candy"))
            val task = if (round == 0) "savings_01" else "savings_02"
            val input = if (round == 0) TaskInput.Deposit(30) else TaskInput.Distribution(mapOf("wants" to 40))
            expected = (game.submitTask(expected, task, input) as TaskResult.Submitted).state
            apply(game.deposit(expected, 15))
            apply(game.withdraw(expected, 5))
            apply(game.closePeriod(expected))
        }
        val actual = RoomGameStorage(db.gameDao()).load(id)!!.state

        // createdAt у операций разный: сравниваем без времени.
        assertEquals(expected.copy(ledger = expected.ledger.map { it.copy(createdAt = 0) }, attempts = expected.attempts.map { it.copy(createdAt = 0) }),
            actual.copy(ledger = actual.ledger.map { it.copy(createdAt = 0) }, attempts = actual.attempts.map { it.copy(createdAt = 0) }))
    }

    @Test
    fun deletingProfileRemovesAllItsRows() = runBlocking {
        val store = store(db)
        val look = PetAppearance(PetCharacter.PUSHISTIK, BodyColor.B, EyesVariant.SLY)
        val first = (store.createProfile("Финни", look) as ProfileResult.Saved).profileId
        val second = (store.createProfile("Бублик", look) as ProfileResult.Saved).profileId
        playTwoPeriods(store, first)

        store.deleteProfile(first)

        assertNull(RoomGameStorage(db.gameDao()).load(first))
        val cursor = db.openHelper.readableDatabase.query("SELECT (SELECT COUNT(*) FROM ledger WHERE profileId = $first) + (SELECT COUNT(*) FROM periods WHERE profileId = $first) + (SELECT COUNT(*) FROM task_attempts WHERE profileId = $first) + (SELECT COUNT(*) FROM pet_state WHERE profileId = $first)")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()
        assertEquals(listOf(second), store.observeProfiles().first().map { it.id })

        store.deleteAll()
        assertTrue(store.observeProfiles().first().isEmpty())
    }
}
