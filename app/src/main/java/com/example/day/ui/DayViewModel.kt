package com.example.day.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day.data.QuoteManager
import com.example.day.data.Task
import com.example.day.data.TaskDao
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.day.data.TemplateTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val taskDao: TaskDao,
    val quoteManager: QuoteManager,
    private val appContext: Context
) : ViewModel() {

    val selectedDate = MutableStateFlow(getTodayDateString())

    val tasks: StateFlow<List<Task>> = selectedDate
        .flatMapLatest { date -> taskDao.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templateTasks: StateFlow<List<TemplateTask>> = taskDao.getAllTemplateTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentQuote = MutableStateFlow("")
    val isDailyQuoteEnabled = MutableStateFlow(true)
    val personalQuote = MutableStateFlow("")

    init {
        isDailyQuoteEnabled.value = quoteManager.isDailyQuoteEnabled()
        personalQuote.value = quoteManager.getPersonalQuote()
        refreshQuote()

        // Observe selectedDate and initialize day if needed
        viewModelScope.launch {
            selectedDate.collect { date ->
                checkAndInitializeDate(date)
            }
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    fun refreshQuote() {
        currentQuote.value = quoteManager.getCurrentQuote()
    }

    fun changeDate(date: String) {
        selectedDate.value = date
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            h * 60 + m
        } catch (e: Exception) {
            0
        }
    }

    private fun checkAndInitializeDate(date: String) {
        viewModelScope.launch {
            if (!quoteManager.isDateInitialized(date)) {
                val templates = taskDao.getAllTemplateTasksSync()
                templates.forEach { template ->
                    val plannedMin = parseTimeToMinutes(template.endTime) - parseTimeToMinutes(template.startTime)
                    val task = Task(
                        name = template.name,
                        startTime = template.startTime,
                        endTime = template.endTime,
                        colorHex = template.colorHex,
                        isCompleted = false,
                        date = date,
                        templateTaskId = template.id,
                        category = template.category,
                        plannedDurationMinutes = plannedMin,
                        actualDurationMinutes = plannedMin
                    )
                    taskDao.insertTask(task)
                }
                quoteManager.setDateInitialized(date)
                updateWidget()
            }
        }
    }

    fun addTask(name: String, startTime: String, endTime: String, colorHex: String) {
        viewModelScope.launch {
            val plannedMin = parseTimeToMinutes(endTime) - parseTimeToMinutes(startTime)
            val task = Task(
                name = name,
                startTime = startTime,
                endTime = endTime,
                colorHex = colorHex,
                date = selectedDate.value,
                templateTaskId = null,
                category = "General",
                plannedDurationMinutes = plannedMin,
                actualDurationMinutes = plannedMin
            )
            taskDao.insertTask(task)
            updateWidget()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            val actualMin = parseTimeToMinutes(task.endTime) - parseTimeToMinutes(task.startTime)
            val updatedTask = task.copy(actualDurationMinutes = actualMin)
            taskDao.updateTask(updatedTask)
            updateWidget()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updatedTask)
            updateWidget()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            updateWidget()
        }
    }

    // Template CRUD Operations
    fun addTemplateTask(name: String, startTime: String, endTime: String, colorHex: String, category: String) {
        viewModelScope.launch {
            val template = TemplateTask(
                name = name,
                startTime = startTime,
                endTime = endTime,
                colorHex = colorHex,
                category = category
            )
            val newId = taskDao.insertTemplateTask(template)
            
            // Retroactively insert task if today is initialized but doesn't have it yet?
            // Actually, we don't need to do it retrospectively unless we want, but auto-fill is for new days.
        }
    }

    fun updateTemplateTask(template: TemplateTask) {
        viewModelScope.launch {
            taskDao.updateTemplateTask(template)
        }
    }

    fun deleteTemplateTask(template: TemplateTask) {
        viewModelScope.launch {
            taskDao.deleteTemplateTask(template)
        }
    }

    fun updateQuoteSettings(isDaily: Boolean, personal: String) {
        quoteManager.setDailyQuoteEnabled(isDaily)
        quoteManager.setPersonalQuote(personal)
        isDailyQuoteEnabled.value = isDaily
        personalQuote.value = personal
        refreshQuote()
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                com.example.day.widget.DayWidget().updateAll(appContext)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
