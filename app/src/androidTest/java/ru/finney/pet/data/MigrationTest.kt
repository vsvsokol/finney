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
import ru.finney.pet.domain.model.PetCharacter

/**
 * ТЗ п. 2.5.13: обновление приложения не стирает прогресс. Каждая миграция
 * проверяется на настоящей базе предыдущей версии — схемы лежат в app/schemas.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinneyDatabase::class.java,
    )

    /** Профили, созданные до выбора питомца, остаются на [@Lix2w78]: до версии 2 рисовалась только она. */
    @Test
    fun migration1To2KeepsProfileAndSetsZalina() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO profiles (id, petName, bodyColor, eyes, activeGoalId, isDemo, createdAt) " +
                    "VALUES (1, 'Финни', 'B', 'SLY', NULL, 0, 100)",
            )
            db.execSQL("INSERT INTO pet_state (profileId, satiety, hygiene, mood) VALUES (1, 70, 70, 70)")
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, FinneyDatabase.MIGRATION_1_2)

        db.query("SELECT petName, petCharacter, bodyColor, eyes FROM profiles").use { cursor ->
            assertTrue("профиль пропал при миграции", cursor.moveToFirst())
            assertEquals("Финни", cursor.getString(0))
            assertEquals(PetCharacter.PUSHISTIK.name, cursor.getString(1))
            assertEquals("B", cursor.getString(2))
            assertEquals("SLY", cursor.getString(3))
        }
        db.close()
    }
}
