package com.hfuu.schedule.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hfuu.schedule.data.model.AgendaEventEntity
import com.hfuu.schedule.data.model.CourseEntity
import com.hfuu.schedule.data.model.ScheduleMemoEntity
import com.hfuu.schedule.data.model.StudyTaskEntity

@Database(
    entities = [
        CourseEntity::class,
        StudyTaskEntity::class,
        ScheduleMemoEntity::class,
        AgendaEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun studyDao(): StudyDao
    abstract fun memoDao(): MemoDao
    abstract fun agendaDao(): AgendaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `schedule_memos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `week` INTEGER NOT NULL,
                        `dayOfWeek` INTEGER NOT NULL,
                        `startSection` INTEGER NOT NULL,
                        `endSection` INTEGER NOT NULL,
                        `isDone` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agenda_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `isAllDay` INTEGER NOT NULL,
                        `startMillis` INTEGER NOT NULL,
                        `endMillis` INTEGER NOT NULL,
                        `repeatRule` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `countdownEnabled` INTEGER NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `reminderMinutesBefore` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hfuu_schedule.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
