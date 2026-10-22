package com.example.day.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.R
import com.example.day.ui.DayViewModel
import com.example.day.ui.agent.AgentScreen
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTabBar
import com.example.day.ui.common.NeuTabItem
import com.example.day.ui.habit.HabitsScreen
import com.example.day.ui.notes.NotesScreen
import com.example.day.ui.settings.SettingsDialog
import com.example.day.ui.today.TodayScreen
import com.example.day.ui.todo.TodoScreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: DayViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isImeVisible = WindowInsets.isImeVisible

    val navItems = listOf(
        NeuTabItem("Today", Icons.Default.Today, 0),
        NeuTabItem("Todo", Icons.Default.Checklist, 1),
        NeuTabItem("Day", R.drawable.ic_day_logo, 2),
        NeuTabItem("Habits", Icons.Default.Eco, 3),
        NeuTabItem("Notes", Icons.Default.EditNote, 4)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isImeVisible) {
                NeuTabBar(
                    items = navItems,
                    selectedIndex = selectedTab,
                    onTabSelected = { tabIndex ->
                        if (selectedTab != tabIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = tabIndex
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(120)).togetherWith(fadeOut(animationSpec = tween(90)))
                },
                label = "TabTransition",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    0 -> TodayScreen(viewModel = viewModel, onOpenSettings = { showSettings = true })
                    1 -> TodoScreen(viewModel = viewModel, onOpenSettings = { showSettings = true })
                    2 -> AgentScreen(dayViewModel = viewModel, onOpenSettings = { showSettings = true })
                    3 -> HabitsScreen(viewModel = viewModel, onOpenSettings = { showSettings = true })
                    4 -> NotesScreen(viewModel = viewModel, onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }
}
