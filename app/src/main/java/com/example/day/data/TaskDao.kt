package com.example.day.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY startTime ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY startTime ASC")
    suspend fun getTasksForDateSync(date: String): List<Task>

    @Query("SELECT * FROM tasks ORDER BY date ASC, startTime ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM template_tasks ORDER BY startTime ASC")
    fun getAllTemplateTasks(): Flow<List<TemplateTask>>

    @Query("SELECT * FROM template_tasks ORDER BY startTime ASC")
    suspend fun getAllTemplateTasksSync(): List<TemplateTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateTask(task: TemplateTask): Long

    @Update
    suspend fun updateTemplateTask(task: TemplateTask)

    @Delete
    suspend fun deleteTemplateTask(task: TemplateTask)
}
