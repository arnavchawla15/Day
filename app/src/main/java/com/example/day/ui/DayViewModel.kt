package com.example.day.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day.data.QuoteManager
import com.example.day.data.Task
import com.example.day.data.TaskDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val taskDao: TaskDao,
    private val quoteManager: QuoteManager
) : ViewModel() {

    val selectedDate = MutableStateFlow(getTodayDateString())

    val tasks: StateFlow<List<Task>> = selectedDate
        .flatMapLatest { date -> taskDao.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentQuote = MutableStateFlow("")
    val isDailyQuoteEnabled = MutableStateFlow(true)
    val personalQuote = MutableStateFlow("")

    init {
        isDailyQuoteEnabled.value = quoteManager.isDailyQuoteEnabled()
        personalQuote.value = quoteManager.getPersonalQuote()
        refreshQuote()
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

    fun addTask(name: String, startTime: String, endTime: String, colorHex: String) {
        viewModelScope.launch {
            val task = Task(
                name = name,
                startTime = startTime,
                endTime = endTime,
                colorHex = colorHex,
                date = selectedDate.value
            )
            taskDao.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updatedTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    fun updateQuoteSettings(isDaily: Boolean, personal: String) {
        quoteManager.setDailyQuoteEnabled(isDaily)
        quoteManager.setPersonalQuote(personal)
        isDailyQuoteEnabled.value = isDaily
        personalQuote.value = personal
        refreshQuote()
    }
}
