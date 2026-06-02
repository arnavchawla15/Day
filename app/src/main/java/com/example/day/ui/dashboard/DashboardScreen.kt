package com.example.day.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.Task
import com.example.day.ui.DayViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DayViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val isDailyEnabled by viewModel.isDailyQuoteEnabled.collectAsState()
    val personalQuote by viewModel.personalQuote.collectAsState()

    var customQuoteText by remember(personalQuote) { mutableStateOf(personalQuote) }
    val scrollState = rememberScrollState()

    // Group tasks by date
    val tasksByDate = remember(allTasks) {
        allTasks.groupBy { it.date }
    }

    // Calculations
    val totalTasksCount = allTasks.size
    val totalCompletedCount = allTasks.count { it.isCompleted }
    val completionRate = if (totalTasksCount > 0) {
        (totalCompletedCount.toFloat() / totalTasksCount * 100).toInt()
    } else {
        0
    }

    // Calculate streaks
    val streakStats = remember(tasksByDate) {
        calculateStreaks(tasksByDate)
    }

    // Warm linear gradient for the current streak card
    val streakGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFE8C00), // Orange
                Color(0xFFF83600)  // Fiery Red
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Progress & Insights",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp
        )

        // Streak Card Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Current Streak",
                value = "${streakStats.current} days",
                icon = Icons.Default.Whatshot,
                backgroundBrush = streakGradient,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Max Streak",
                value = "${streakStats.max} days",
                icon = Icons.Default.EmojiEvents,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Completion Rate",
                value = "$completionRate%",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Tasks",
                value = "$totalCompletedCount/$totalTasksCount",
                icon = Icons.Default.List,
                modifier = Modifier.weight(1f)
            )
        }

        // LeetCode activity grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Activity Graph",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your daily completed tasks distribution (past 16 weeks)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                ActivityGrid(tasksByDate = tasksByDate)

                // Grid Legend (using rounded squares shape 4.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDark = isSystemInDarkTheme()
                    Text(text = "Less ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF21262D) else Color(0xFFF5F5F5)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF0E4429) else Color(0xFFE2F1E7)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF006D32) else Color(0xFF8FE1A2)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF26A641) else Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(4.dp)).background(if (isDark) Color(0xFF39D353) else Color(0xFF1B5E20)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = " More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        // Quote configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Motivational Quote Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Rotating Quote",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Enable to rotate a motivational quote every 24 hours.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isDailyEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.updateQuoteSettings(isChecked, customQuoteText)
                        }
                    )
                }

                if (!isDailyEnabled) {
                    OutlinedTextField(
                        value = customQuoteText,
                        onValueChange = { customQuoteText = it },
                        label = { Text("Personal Constant Quote") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.updateQuoteSettings(false, customQuoteText)
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Save Quote")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundBrush: Brush? = null
) {
    val textColor = if (backgroundBrush != null) Color.White else MaterialTheme.colorScheme.onSurface
    val iconTint = if (backgroundBrush != null) Color.White else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundBrush != null) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val baseModifier = if (backgroundBrush != null) {
            Modifier.background(backgroundBrush)
        } else Modifier

        Row(
            modifier = Modifier
                .then(baseModifier)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(0.85f)
            )
        }
    }
}

@Composable
fun ActivityGrid(
    tasksByDate: Map<String, List<Task>>,
    modifier: Modifier = Modifier
) {
    val columnsCount = 16
    val daysCount = columnsCount * 7

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val datesGrid = remember(tasksByDate) {
        val calendar = Calendar.getInstance()
        // Align to the end of the current week (Saturday)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        // Step back by 16 weeks
        calendar.add(Calendar.DAY_OF_YEAR, -daysCount + 1)

        val dates = mutableListOf<String>()
        for (i in 0 until daysCount) {
            dates.add(sdf.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        dates
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Week day letters on the left (e.g. S M T W T F S)
        val dayLetters = listOf("S", "", "T", "", "T", "", "S")
        Column(
            modifier = Modifier.height(112.dp), // 7 rows * 14.dp + 6 gaps * 2.dp = 98 + 12 = 110.dp
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            dayLetters.forEach { letter ->
                Box(
                    modifier = Modifier.size(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Grid itself (using rounded squares shape 4.dp)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (w in 0 until columnsCount) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (d in 0 until 7) {
                        val index = w * 7 + d
                        val date = datesGrid.getOrNull(index) ?: ""
                        val tasksOnDate = tasksByDate[date] ?: emptyList()
                        val completedCount = tasksOnDate.count { it.isCompleted }

                        val isDark = isSystemInDarkTheme()
                        val color = when {
                            completedCount == 0 -> if (isDark) Color(0xFF21262D) else Color(0xFFF5F5F5)
                            completedCount == 1 -> if (isDark) Color(0xFF0E4429) else Color(0xFFE2F1E7)
                            completedCount == 2 -> if (isDark) Color(0xFF006D32) else Color(0xFF8FE1A2)
                            completedCount == 3 -> if (isDark) Color(0xFF26A641) else Color(0xFF4CAF50)
                            else -> if (isDark) Color(0xFF39D353) else Color(0xFF1B5E20)
                        }

                        val borderModifier = if (completedCount == 0) {
                            Modifier.border(0.5.dp, if (isDark) Color(0xFF30363D) else Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .then(borderModifier)
                        )
                    }
                }
            }
        }
    }
}

// Data structures for streaks
data class StreakStats(val current: Int, val max: Int)

private fun calculateStreaks(tasksByDate: Map<String, List<Task>>): StreakStats {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Convert keys to Date and filter only dates where ALL tasks are completed and totalTasks > 0
    val completedDates = tasksByDate.entries
        .filter { entry -> entry.value.isNotEmpty() && entry.value.all { it.isCompleted } }
        .mapNotNull { entry ->
            try {
                sdf.parse(entry.key)
            } catch (e: Exception) {
                null
            }
        }
        .map { date ->
            val cal = Calendar.getInstance()
            cal.time = date
            // Strip time
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        .toSet()

    if (completedDates.isEmpty()) {
        return StreakStats(0, 0)
    }

    // Sort dates descending
    val todayCal = Calendar.getInstance()
    todayCal.set(Calendar.HOUR_OF_DAY, 0)
    todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0)
    todayCal.set(Calendar.MILLISECOND, 0)
    val todayMillis = todayCal.timeInMillis

    val yesterdayCal = Calendar.getInstance()
    yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
    yesterdayCal.set(Calendar.HOUR_OF_DAY, 0)
    yesterdayCal.set(Calendar.MINUTE, 0)
    yesterdayCal.set(Calendar.SECOND, 0)
    yesterdayCal.set(Calendar.MILLISECOND, 0)
    val yesterdayMillis = yesterdayCal.timeInMillis

    var currentStreak = 0
    var checkMillis = todayMillis

    if (completedDates.contains(todayMillis)) {
        currentStreak = 1
        checkMillis = todayMillis
    } else if (completedDates.contains(yesterdayMillis)) {
        currentStreak = 1
        checkMillis = yesterdayMillis
    }

    if (currentStreak > 0) {
        val checkCal = Calendar.getInstance()
        checkCal.timeInMillis = checkMillis
        while (true) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val prevDayMillis = checkCal.timeInMillis
            if (completedDates.contains(prevDayMillis)) {
                currentStreak++
            } else {
                break
            }
        }
    }

    // Calculate max streak in history
    val sortedDatesAsc = completedDates.sorted()
    var maxStreak = 0
    var tempStreak = 0
    var prevMillis: Long? = null

    sortedDatesAsc.forEach { dateMillis ->
        if (prevMillis == null) {
            tempStreak = 1
        } else {
            val expectedNextCal = Calendar.getInstance()
            expectedNextCal.timeInMillis = prevMillis!!
            expectedNextCal.add(Calendar.DAY_OF_YEAR, 1)
            val expectedMillis = expectedNextCal.timeInMillis

            if (dateMillis == expectedMillis) {
                tempStreak++
            } else {
                maxStreak = maxOf(maxStreak, tempStreak)
                tempStreak = 1
            }
        }
        prevMillis = dateMillis
    }
    maxStreak = maxOf(maxStreak, tempStreak)

    return StreakStats(currentStreak, maxStreak)
}
