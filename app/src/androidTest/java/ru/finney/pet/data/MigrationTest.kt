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

/** Обновление приложения не стирает прогресс: база версии 3 переносится в 4 с данными. */
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
}
