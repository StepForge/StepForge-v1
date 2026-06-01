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
        WaterIntakeEvent::class,
        WorkoutSession::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun dailyWaterDao(): DailyWaterDao
    abstract fun hourlyStepsDao(): HourlyStepsDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun sleepStageDao(): SleepStageDao
    abstract fun waterIntakeEventDao(): WaterIntakeEventDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

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

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        durationMinutes INTEGER NOT NULL,
                        steps INTEGER NOT NULL,
                        distanceMeters INTEGER NOT NULL,
                        caloriesKcal INTEGER NOT NULL,
                        avgStepsPerMinute INTEGER NOT NULL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_workout_session_date_startTime ON workout_session(date, startTime)"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sleep_session ADD COLUMN type TEXT NOT NULL DEFAULT 'main'")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // version increase only to clear corrupt data
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_steps ADD COLUMN source TEXT NOT NULL DEFAULT 'sensor'")
                database.execSQL("ALTER TABLE hourly_steps ADD COLUMN source TEXT NOT NULL DEFAULT 'sensor'")
            }
        }

        private fun hasColumn(
            database: SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            val cursor = database.query("PRAGMA table_info($tableName)")
            return try {
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
                false
            } finally {
                cursor.close()
            }
        }

        private fun ensureSleepSessionNotesColumn(database: SupportSQLiteDatabase) {
            if (!hasColumn(database, "sleep_session", "notes")) {
                database.execSQL("ALTER TABLE sleep_session ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13_ADD_SLEEP_SESSION_NOTES = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSleepSessionNotesColumn(database)
            }
        }

        private val MIGRATION_13_14_ENSURE_SLEEP_SESSION_NOTES = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSleepSessionNotesColumn(database)
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
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13_ADD_SLEEP_SESSION_NOTES,
                        MIGRATION_13_14_ENSURE_SLEEP_SESSION_NOTES
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}