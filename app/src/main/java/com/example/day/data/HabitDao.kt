package com.example.day.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAllHabitsSync(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // Habit logs
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date >= :startDate ORDER BY date DESC")
    fun getLogsForHabit(habitId: Int, startDate: String): Flow<List<HabitLog>>

    @Query("SELECT date, SUM(durationMinutes) as totalMinutes FROM habit_logs WHERE habitId = :habitId AND date >= :startDate GROUP BY date")
    fun getDailyTotalsForHabit(habitId: Int, startDate: String): Flow<List<DailyHabitTotal>>

    @Query("SELECT date, SUM(durationMinutes) as totalMinutes FROM habit_logs WHERE habitId = :habitId AND date >= :startDate GROUP BY date")
    suspend fun getDailyTotalsForHabitSync(habitId: Int, startDate: String): List<DailyHabitTotal>

    @Query("SELECT * FROM habit_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs ORDER BY date DESC")
    suspend fun getAllLogsSync(): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLog): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteHabitLog(habitId: Int, date: String)
}
