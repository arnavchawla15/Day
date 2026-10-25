package com.example.day.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Task::class,
        TemplateTask::class,
        Todo::class,
        Habit::class,
        HabitLog::class,
        Note::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun todoDao(): TodoDao
    abstract fun habitDao(): HabitDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("PRAGMA table_info(todos)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val colIndex = cursor.getColumnIndex("name")
                    if (colIndex >= 0) {
                        columns.add(cursor.getString(colIndex))
                    }
                }
                cursor.close()
                if (!columns.contains("recurrenceType")) {
                    database.execSQL("ALTER TABLE todos ADD COLUMN recurrenceType TEXT")
                }
                if (!columns.contains("recurrenceDays")) {
                    database.execSQL("ALTER TABLE todos ADD COLUMN recurrenceDays TEXT")
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("PRAGMA table_info(habit_logs)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val colIndex = cursor.getColumnIndex("name")
                    if (colIndex >= 0) {
                        columns.add(cursor.getString(colIndex))
                    }
                }
                cursor.close()
                if (!columns.contains("durationMinutes")) {
                    database.execSQL("ALTER TABLE habit_logs ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "day_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
