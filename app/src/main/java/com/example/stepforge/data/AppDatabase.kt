package com.example.stepforge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailySteps::class,
        DailyWater::class,
        HourlySteps::class,
        SleepSession::class,
        SleepStage::class,
        WaterIntakeEvent::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun dailyWaterDao(): DailyWaterDao
    abstract fun hourlyStepsDao(): HourlyStepsDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun sleepStageDao(): SleepStageDao
    abstract fun waterIntakeEventDao(): WaterIntakeEventDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) { /* no-op */ }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) { /* no-op */ }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hourly_steps (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        hour INTEGER NOT NULL,
                        steps INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sleep_session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        totalMinutes INTEGER NOT NULL,
                        qualityScore INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sleep_stage (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        stageType TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE sleep_session
                    ADD COLUMN source TEXT NOT NULL DEFAULT 'manual'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hourly_steps_new (
                        date TEXT NOT NULL,
                        hour INTEGER NOT NULL,
                        steps INTEGER NOT NULL,
                        PRIMARY KEY(date, hour)
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT OR REPLACE INTO hourly_steps_new (date, hour, steps)
                    SELECT date, hour, MAX(steps) as steps
                    FROM hourly_steps
                    GROUP BY date, hour
                    """.trimIndent()
                )

                database.execSQL("DROP TABLE hourly_steps")
                database.execSQL("ALTER TABLE hourly_steps_new RENAME TO hourly_steps")
            }
        }

        // ✅ 7 -> 8: water intake events table
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS water_intake_event (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        timeMillis INTEGER NOT NULL,
                        amountMl INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_water_intake_event_date_timeMillis ON water_intake_event(date, timeMillis)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stepforge_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}