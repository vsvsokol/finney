package ru.finney.pet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.finney.pet.domain.model.PetCharacter

/**
 * Схема каждой версии экспортируется в app/schemas и коммитится.
 * Меняешь entity — поднимаешь [version] и пишешь миграцию: у экспертов не должен пропасть прогресс.
 */
@Database(
    entities = [
        ProfileEntity::class,
        PetStateEntity::class,
        PeriodEntity::class,
        LedgerEntryEntity::class,
        TaskAttemptEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class FinneyDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        const val NAME = "finney.db"

        /**
         * Выбор питомца (ТЗ п. 2.5.2). У кого профиль создан до выбора — [@Lix2w78]:
         * до этой версии рисовалась только она.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE profiles ADD COLUMN petCharacter TEXT NOT NULL " +
                        "DEFAULT '${PetCharacter.PUSHISTIK.name}'",
                )
            }
        }

        fun create(context: Context): FinneyDatabase =
            Room.databaseBuilder(context.applicationContext, FinneyDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
