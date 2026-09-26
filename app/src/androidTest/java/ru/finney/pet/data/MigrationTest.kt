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

/** Обновление приложения не стирает прогресс: база версии 3 переносится в 4 и 5 с данными. */
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
}
