package com.example.day.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, priority ASC, dueDate ASC, id DESC")
    fun getAllTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY priority ASC, dueDate ASC, id DESC")
    fun getActiveTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY id DESC")
    fun getCompletedTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Int): Todo?

    @Query("SELECT * FROM todos WHERE dueDate = :date ORDER BY isCompleted ASC, priority ASC")
    suspend fun getTodosForDateSync(date: String): List<Todo>

    @Query("SELECT * FROM todos WHERE (dueDate = :date OR dueDate IS NULL OR dueDate < :date) ORDER BY isCompleted ASC, priority ASC, dueDate ASC")
    suspend fun getTodosForWidgetSync(date: String): List<Todo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo): Long

    @Update
    suspend fun updateTodo(todo: Todo)

    @Delete
    suspend fun deleteTodo(todo: Todo)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Int)
}
