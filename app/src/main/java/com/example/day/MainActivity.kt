package com.example.day

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day.data.AppDatabase
import com.example.day.data.QuoteManager
import com.example.day.reminder.ReminderManager
import com.example.day.theme.DayTheme
import com.example.day.ui.DayViewModel

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            ReminderManager.rescheduleAllReminders(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize reminders and notification channel
        ReminderManager.createNotificationChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ReminderManager.rescheduleAllReminders(applicationContext)
            }
        } else {
            ReminderManager.rescheduleAllReminders(applicationContext)
        }

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
