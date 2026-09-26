package ru.finney.pet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Схема каждой версии экспортируется в app/schemas и коммитится.
 *
 * С версии 3 изменения переносятся миграциями — сейчас это [MIGRATION_3_4]. Базы версий 1–2
 * пересоздаются с нуля: версия 3 переименовала питомцев, а перенести старые значения было бы
 * возможно только храня прежние имена прямо в коде.
 */
@Database(
    entities = [
        ProfileEntity::class,
        PetStateEntity::class,
        PeriodEntity::class,
        LedgerEntryEntity::class,
        TaskAttemptEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FinneyDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        const val NAME = "finney.db"

        fun create(context: Context): FinneyDatabase =
            Room.databaseBuilder(context.applicationContext, FinneyDatabase::class.java, NAME)
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        /**
         * 3 → 4: надетый аксессуар. Колонка допускает NULL — у всех существующих
         * профилей ничего не надето, остальные данные не трогаются.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN wornItemId TEXT")
            }
        }
    }
}
