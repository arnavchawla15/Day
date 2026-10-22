package com.example.day

import com.example.day.data.RecurrenceHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecurrenceHelperTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getTodayStr(): String {
        return sdf.format(Calendar.getInstance().time)
    }

    @Test
    fun testDailyRecurrence() {
        val baseDate = getTodayStr()
        val next = RecurrenceHelper.calculateNextDueDate(baseDate, RecurrenceHelper.TYPE_DAILY, null)
        val cal = Calendar.getInstance().apply {
            time = sdf.parse(baseDate)!!
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val expected = sdf.format(cal.time)
        assertEquals(expected, next)
    }

    @Test
    fun testEveryOtherDayRecurrence() {
        val baseDate = getTodayStr()
        val next = RecurrenceHelper.calculateNextDueDate(baseDate, RecurrenceHelper.TYPE_EVERY_OTHER_DAY, null)
        val cal = Calendar.getInstance().apply {
            time = sdf.parse(baseDate)!!
            add(Calendar.DAY_OF_MONTH, 2)
        }
        val expected = sdf.format(cal.time)
        assertEquals(expected, next)
    }

    @Test
    fun testWeeklyRecurrence() {
        val baseDate = getTodayStr()
        val next = RecurrenceHelper.calculateNextDueDate(baseDate, RecurrenceHelper.TYPE_WEEKLY, "Sunday")
        val nextCal = Calendar.getInstance().apply {
            time = sdf.parse(next)!!
        }
        assertEquals(Calendar.SUNDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertTrue(nextCal.timeInMillis > sdf.parse(baseDate)!!.time)
    }

    @Test
    fun testCustomDaysRecurrence() {
        val baseDate = getTodayStr()
        val next = RecurrenceHelper.calculateNextDueDate(baseDate, RecurrenceHelper.TYPE_CUSTOM, "Mon,Wed,Fri")
        val nextCal = Calendar.getInstance().apply {
            time = sdf.parse(next)!!
        }
        val targetSet = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        assertTrue(targetSet.contains(nextCal.get(Calendar.DAY_OF_WEEK)))
        assertTrue(nextCal.timeInMillis > sdf.parse(baseDate)!!.time)
    }

    @Test
    fun testGetNextDueDateWithTodo() {
        val todo = com.example.day.data.Todo(
            title = "Gym",
            dueDate = getTodayStr(),
            recurrenceType = "DAILY"
        )
        assertTrue(RecurrenceHelper.isRecurring(todo))
        val nextDue = RecurrenceHelper.getNextDueDate(todo)
        val cal = Calendar.getInstance().apply {
            time = sdf.parse(todo.dueDate!!)!!
            add(Calendar.DAY_OF_MONTH, 1)
        }
        assertEquals(sdf.format(cal.time), nextDue)
    }

    @Test
    fun testNonRecurringTodo() {
        val todo = com.example.day.data.Todo(
            title = "One-off task",
            dueDate = getTodayStr(),
            recurrenceType = "NONE"
        )
        org.junit.Assert.assertFalse(RecurrenceHelper.isRecurring(todo))
    }
}
