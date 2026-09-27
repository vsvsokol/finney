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
 * С версии 3 изменения переносятся миграциями — [MIGRATION_3_4] … [MIGRATION_5_6]. Базы версий 1–2
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
    version = 6,
    exportSchema = true,
)
abstract class FinneyDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        const val NAME = "finney.db"

        fun create(context: Context): FinneyDatabase =
            Room.databaseBuilder(context.applicationContext, FinneyDatabase::class.java, NAME)
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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

        /**
         * Сон у питомцев, созданных до версии 5. Столько же, сколько у нового питомца после
         * первого спада (90 − 60): потребность уже видна, но питомец ещё не «устал».
         */
        const val ENERGY_ON_MIGRATION = "30"

        /** 4 → 5: шкала сна и время, когда питомец лёг спать (никто не спит). Остальное не трогается. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pet_state ADD COLUMN energy INTEGER NOT NULL DEFAULT $ENERGY_ON_MIGRATION")
                db.execSQL("ALTER TABLE pet_state ADD COLUMN sleepingSince INTEGER")
            }
        }

        /**
         * 5 → 6: игра уровня. У старых периодов её нет: закрытые считаются пройденными
         * по игре (NULL читается как true), в текущем игра не обязательна.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE periods ADD COLUMN levelTaskId TEXT")
                db.execSQL("ALTER TABLE periods ADD COLUMN gamePassed INTEGER")
            }
        }
    }
}
