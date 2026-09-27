package ru.finney.pet.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.finney.pet.data.db.FinneyDatabase

/** Обновление приложения не стирает прогресс: база версии 3 переносится в 4, 5, 6 и 7 с данными. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FinneyDatabase::class.java)

    @Test
    fun migrate3To4KeepsProfileAndAddsEmptyWornItem() {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO profiles (id, petName, petCharacter, bodyColor, eyes, activeGoalId, isDemo, createdAt) " +
                    "VALUES (1, 'Финни', 'PUSHISTIK', 'A', 'ROUND', 'goal_bike', 0, 1)",
            )
        }

        helper.runMigrationsAndValidate(dbName, 4, true, FinneyDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT petName, activeGoalId, wornItemId FROM profiles WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Финни", c.getString(0))
                assertEquals("goal_bike", c.getString(1))
                assertTrue("после миграции ничего не надето", c.isNull(2))
            }
        }
    }

    @Test
    fun migrate4To5KeepsStatsAndAddsEnergy() {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL(
                "INSERT INTO profiles (id, petName, petCharacter, bodyColor, eyes, activeGoalId, isDemo, createdAt, wornItemId) " +
                    "VALUES (1, 'Финни', 'PUSHISTIK', 'A', 'ROUND', NULL, 0, 1, 'hat_cowboy')",
            )
            db.execSQL("INSERT INTO pet_state (profileId, satiety, hygiene, mood) VALUES (1, 55, 44, 33)")
        }

        helper.runMigrationsAndValidate(dbName, 5, true, FinneyDatabase.MIGRATION_4_5).use { db ->
            db.query("SELECT satiety, hygiene, mood, energy, sleepingSince FROM pet_state WHERE profileId = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(55, c.getInt(0))
                assertEquals(44, c.getInt(1))
                assertEquals(33, c.getInt(2))
                assertEquals(FinneyDatabase.ENERGY_ON_MIGRATION.toInt(), c.getInt(3))
                assertTrue("после обновления никто не спит", c.isNull(4))
            }
            db.query("SELECT wornItemId FROM profiles WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("hat_cowboy", c.getString(0))
            }
        }
    }

    @Test
    fun migrate5To6KeepsPeriodsWithoutLevelGame() {
        helper.createDatabase(dbName, 5).use { db ->
            db.execSQL(
                "INSERT INTO profiles (id, petName, petCharacter, bodyColor, eyes, activeGoalId, isDemo, createdAt, wornItemId) " +
                    "VALUES (1, 'Финни', 'PUSHISTIK', 'A', 'ROUND', NULL, 0, 1, NULL)",
            )
            db.execSQL(
                "INSERT INTO periods (profileId, number, stage, phase, needsCovered, planMatched, savingsAdded, " +
                    "successfulTasks, pointsEarned) VALUES (1, 1, 1, 'CLOSED', 1, 1, 0, 1, 5)",
            )
        }

        helper.runMigrationsAndValidate(dbName, 6, true, FinneyDatabase.MIGRATION_5_6).use { db ->
            db.query("SELECT pointsEarned, levelTaskId, gamePassed FROM periods WHERE profileId = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(5, c.getInt(0))
                assertTrue("у старого периода игры уровня нет", c.isNull(1))
                assertTrue("итог по игре не записан — читается как пройдено", c.isNull(2))
            }
        }
    }

    @Test
    fun migrate6To7KeepsPetAndToysAndStartsWithoutPlaySession() {
        helper.createDatabase(dbName, 6).use { db ->
            db.execSQL(
                "INSERT INTO profiles (id, petName, petCharacter, bodyColor, eyes, activeGoalId, isDemo, createdAt, wornItemId) " +
                    "VALUES (1, 'Финни', 'PUSHISTIK', 'A', 'ROUND', NULL, 0, 1, NULL)",
            )
            db.execSQL(
                "INSERT INTO pet_state (profileId, satiety, hygiene, mood, energy, sleepingSince) VALUES (1, 55, 44, 33, 80, NULL)",
            )
            // Мячик, купленный до игрушек, — обычная покупка в ledger: после обновления он в зале.
            db.execSQL(
                "INSERT INTO ledger (profileId, periodNumber, type, balanceDelta, savingsDelta, category, itemId, goalId, " +
                    "taskId, unplanned, createdAt) VALUES (1, 1, 'PURCHASE', -15, 0, 'WANTS', 'toy_ball', NULL, NULL, 0, 2)",
            )
        }

        helper.runMigrationsAndValidate(dbName, 7, true, FinneyDatabase.MIGRATION_6_7).use { db ->
            db.query("SELECT satiety, hygiene, mood, energy, playMood, playSince FROM pet_state WHERE profileId = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(55, c.getInt(0))
                assertEquals(44, c.getInt(1))
                assertEquals(33, c.getInt(2))
                assertEquals(80, c.getInt(3))
                assertEquals("сессии игры ещё не было", 0, c.getInt(4))
                assertTrue(c.isNull(5))
            }
            db.query("SELECT itemId FROM ledger WHERE profileId = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("toy_ball", c.getString(0))
            }
        }
    }
}
