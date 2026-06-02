package com.example.day.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            fontWeight = FontWeight.Bold
        )

        // Streak Card Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Current Streak",
                value = "${streakStats.current} 🔥",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Max Streak",
                value = "${streakStats.max} 🏆",
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
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Tasks",
                value = "$totalCompletedCount/$totalTasksCount",
                modifier = Modifier.weight(1f)
            )
        }

        // LeetCode activity grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                // Grid Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDark = isSystemInDarkTheme()
                    Text(text = "Less ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) Color(0xFF21262D) else Color(0xFFF5F5F5)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) Color(0xFF0E4429) else Color(0xFFE2F1E7)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) Color(0xFF006D32) else Color(0xFF8FE1A2)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) Color(0xFF26A641) else Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) Color(0xFF39D353) else Color(0xFF1B5E20)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = " More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        // Quote configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.updateQuoteSettings(false, customQuoteText)
                        },
                        modifier = Modifier.align(Alignment.End),
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ActivityGrid(
    tasksByDate: Map<String, List<Task>>,
    modifier: Modifier = Modifier
) {
    // We will render 16 weeks of completion history
    // Rows = 7 (Sunday to Saturday)
    // Columns = 16 (weeks)
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

        // Grid itself
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
                            Modifier.border(0.5.dp, if (isDark) Color(0xFF30363D) else Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
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

    // Sort millis descending
    val sortedCompletedList = completedDates.sortedDescending()

    // Calculate current streak starting from today or yesterday
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

    // If today is completed, start from today.
    // If today has no tasks at all, or has tasks but not yet complete, and yesterday is completed, start check from yesterday.
    if (completedDates.contains(todayMillis)) {
        currentStreak = 1
        checkMillis = todayMillis
    } else if (completedDates.contains(yesterdayMillis)) {
        // If today has no tasks or is incomplete, check yesterday
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
