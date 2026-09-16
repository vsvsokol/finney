package ru.finney.pet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 1,
    exportSchema = true,
)
abstract class FinneyDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        const val NAME = "finney.db"

        fun create(context: Context): FinneyDatabase =
            Room.databaseBuilder(context.applicationContext, FinneyDatabase::class.java, NAME).build()
    }
}
