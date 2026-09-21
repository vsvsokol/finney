package ru.finney.pet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Схема каждой версии экспортируется в app/schemas и коммитится.
 *
 * Миграций нет: версия 3 переименовала питомцев, а перенести старые значения было бы
 * возможно только храня прежние имена прямо в коде. База пересоздаётся с нуля —
 * обычный перезапуск прогресс сохраняет, а установка новой сборки поверх старой его стирает.
 */
@Database(
    entities = [
        ProfileEntity::class,
        PetStateEntity::class,
        PeriodEntity::class,
        LedgerEntryEntity::class,
        TaskAttemptEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class FinneyDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        const val NAME = "finney.db"

        fun create(context: Context): FinneyDatabase =
            Room.databaseBuilder(context.applicationContext, FinneyDatabase::class.java, NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
