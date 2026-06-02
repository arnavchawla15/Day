package com.example.day

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.day.data.AppDatabase
import com.example.day.data.QuoteManager
import com.example.day.ui.DayViewModel
import com.example.day.ui.main.MainScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current.applicationContext
  
  val database = remember { AppDatabase.getDatabase(context) }
  val taskDao = remember { database.taskDao() }
  val quoteManager = remember { QuoteManager(context) }
  
  val viewModel: DayViewModel = viewModel {
    DayViewModel(taskDao, quoteManager)
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            viewModel = viewModel,
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
