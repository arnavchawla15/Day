package com.example.day

import com.example.day.data.GeminiRepository
import com.example.day.data.ParsedResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartInputParserTest {

    private val repo = GeminiRepository()

    @Test
    fun testTaskTimeRangeParsing() = runBlocking {
        val result = repo.parseInput(null, "Gym 7-8am")
        assertTrue(result is ParsedResult.TaskResult)
        val task = result as ParsedResult.TaskResult
        assertEquals("Gym", task.title)
        assertEquals("07:00", task.startTime)
        assertEquals("08:00", task.endTime)
    }

    @Test
    fun testTaskTimeRangeToParsing() = runBlocking {
        val result = repo.parseInput(null, "Study 2pm to 4pm")
        assertTrue(result is ParsedResult.TaskResult)
        val task = result as ParsedResult.TaskResult
        assertEquals("Study", task.title)
        assertEquals("14:00", task.startTime)
        assertEquals("16:00", task.endTime)
    }

    @Test
    fun testTodoTomorrowParsing() = runBlocking {
        val result = repo.parseInput(null, "Call mom tomorrow p1")
        assertTrue(result is ParsedResult.TodoResult)
        val todo = result as ParsedResult.TodoResult
        assertTrue(todo.title.contains("Call mom"))
        assertEquals("P1", todo.priority)
    }

    @Test
    fun testHabitParsing() = runBlocking {
        val result = repo.parseInput(null, "track habit Drink Water")
        assertTrue(result is ParsedResult.HabitResult)
        val habit = result as ParsedResult.HabitResult
        assertTrue(habit.name.contains("Drink Water"))
    }

    @Test
    fun testNoteParsing() = runBlocking {
        val result = repo.parseInput(null, "note: remember to buy batteries")
        assertTrue(result is ParsedResult.NoteResult)
        val note = result as ParsedResult.NoteResult
        assertTrue(note.content.contains("buy batteries"))
    }

    @Test
    fun testMultiItemParsing() = runBlocking {
        val multiline = "Gym 7-8am\nDSA 8-10pm\nCall mom tomorrow p1"
        val results = repo.parseMultipleInputs(null, multiline)
        assertEquals(3, results.size)
        assertTrue(results[0] is ParsedResult.TaskResult)
        assertTrue(results[1] is ParsedResult.TaskResult)
        assertTrue(results[2] is ParsedResult.TodoResult)
    }
}
