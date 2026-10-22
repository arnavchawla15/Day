package com.example.day

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.data.AppDatabase
import com.example.day.data.QuoteManager
import com.example.day.theme.DayTheme
import com.example.day.ui.DayViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val taskDao = database.taskDao()
        val todoDao = database.todoDao()
        val habitDao = database.habitDao()
        val noteDao = database.noteDao()
        val quoteManager = QuoteManager(applicationContext)

        enableEdgeToEdge()
        setContent {
            val dayViewModel: DayViewModel = viewModel {
                DayViewModel(
                    taskDao = taskDao,
                    todoDao = todoDao,
                    habitDao = habitDao,
                    noteDao = noteDao,
                    quoteManager = quoteManager,
                    appContext = applicationContext
                )
            }

            val appPrimaryHex by dayViewModel.appPrimaryHex.collectAsState()
            val appDarkMode by dayViewModel.appDarkMode.collectAsState()

            DayTheme(primaryHex = appPrimaryHex, isDark = appDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(viewModel = dayViewModel)
                }
            }
        }
    }
}
