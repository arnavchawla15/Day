package com.example.day.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.data.Task
import com.example.day.data.TemplateTask
import com.example.day.ui.DayViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DayViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val templateTasks by viewModel.templateTasks.collectAsState()

    val scrollState = rememberScrollState()

    // 30 Days Calculation
    val past30Days = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -29)
        for (i in 0 until 30) {
            dates.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        dates
    }

    // 28 Days (4 Weeks) Calculation for mini bar charts
    val past28Days = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -27)
        for (i in 0 until 28) {
            dates.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        dates
    }

    // Current Month Prefix
    val currentMonthPrefix = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)
    }
    val currentMonthDates = remember(past30Days) {
        past30Days.filter { it.startsWith(currentMonthPrefix) }
    }

    // 1. Overall Daily Completion Rates (Past 30 Days)
    val overallDailyCompletionRates = remember(allTasks, templateTasks, past30Days) {
        past30Days.map { dateStr ->
            val dayTasks = allTasks.filter { it.date == dateStr }
            if (templateTasks.isEmpty()) 0f
            else {
                val rates = templateTasks.map { calculateTaskCompletion(it.id, dayTasks) }
                rates.average().toFloat()
            }
        }
    }

    // 2. Trend Calculations
    val trendDirection = remember(overallDailyCompletionRates) {
        if (overallDailyCompletionRates.size < 30) Pair(0f, "Flat")
        else {
            val firstHalfAvg = overallDailyCompletionRates.take(15).average().toFloat()
            val secondHalfAvg = overallDailyCompletionRates.takeLast(15).average().toFloat()
            val diff = (secondHalfAvg - firstHalfAvg) * 100
            when {
                diff > 1f -> Pair(diff, "Up")
                diff < -1f -> Pair(diff, "Down")
                else -> Pair(0f, "Flat")
            }
        }
    }

    // 3. Current Month Average Completion Rate
    val currentMonthAvgCompletion = remember(allTasks, templateTasks, currentMonthDates) {
        if (currentMonthDates.isEmpty() || templateTasks.isEmpty()) 0f
        else {
            val rates = currentMonthDates.flatMap { dateStr ->
                val dayTasks = allTasks.filter { it.date == dateStr }
                templateTasks.map { calculateTaskCompletion(it.id, dayTasks) }
            }
            if (rates.isNotEmpty()) rates.average().toFloat() else 0f
        }
    }

    // 4. Per-Habit Cards Calculations
    val habitCardData = remember(allTasks, templateTasks, past30Days, past28Days) {
        templateTasks.map { template ->
            val dates30Completions = past30Days.map { dateStr ->
                val dayTasks = allTasks.filter { it.date == dateStr }
                calculateTaskCompletion(template.id, dayTasks)
            }

            val dates28Completions = past28Days.map { dateStr ->
                val dayTasks = allTasks.filter { it.date == dateStr }
                calculateTaskCompletion(template.id, dayTasks)
            }

            // Streak calculations
            val streaks = calculateHabitStreaks(dates30Completions)

            // Consistency % (completed days vs planned days)
            val plannedDays = 30
            val completedDaysCount = dates30Completions.count { it >= 0.5f }
            val consistencyPercent = (completedDaysCount.toFloat() / plannedDays.toFloat() * 100).toInt()

            // Average actual duration vs planned duration
            val completedTasks = allTasks.filter { it.templateTaskId == template.id && it.isCompleted }
            val avgActual = if (completedTasks.isNotEmpty()) completedTasks.map { it.actualDurationMinutes }.average().toInt() else 0
            val plannedMin = parseTimeToMinutes(template.endTime) - parseTimeToMinutes(template.startTime)

            HabitCardItem(
                template = template,
                currentStreak = streaks.first,
                maxStreak = streaks.second,
                consistencyPercent = consistencyPercent,
                avgActualMinutes = avgActual,
                plannedMinutes = plannedMin,
                completions28 = dates28Completions
            )
        }
    }

    // 5. Ranked List Calculations (Current Month)
    val rankedHabitList = remember(allTasks, templateTasks, currentMonthDates) {
        templateTasks.map { template ->
            val monthCompletions = currentMonthDates.map { dateStr ->
                val dayTasks = allTasks.filter { it.date == dateStr }
                calculateTaskCompletion(template.id, dayTasks)
            }
            val completedCount = monthCompletions.count { it >= 0.5f }
            val consistency = if (currentMonthDates.isNotEmpty()) {
                completedCount.toFloat() / currentMonthDates.size.toFloat()
            } else 0f

            RankedHabitItem(
                name = template.name,
                colorHex = template.colorHex,
                consistency = consistency
            )
        }.sortedByDescending { it.consistency }
    }

    // Smooth load animation trigger
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { it / 3 },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Habit Insights",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Monthly Overview Donut + Consistency Trend Line Chart Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Donut Card
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Month Completion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        MonthlyOverviewDonutChart(
                            averageCompletionRate = currentMonthAvgCompletion,
                            modifier = Modifier.size(110.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                // Line Chart Card
                Card(
                    modifier = Modifier
                        .weight(1.9f)
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "30-Day Trend",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Overall completion rate",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            
                            // Trend direction indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val trendIcon = when (trendDirection.second) {
                                    "Up" -> Icons.Default.TrendingUp
                                    "Down" -> Icons.Default.TrendingDown
                                    else -> Icons.Default.TrendingFlat
                                }
                                val trendColor = when (trendDirection.second) {
                                    "Up" -> Color(0xFF4CAF50)
                                    "Down" -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                                Icon(
                                    imageVector = trendIcon,
                                    contentDescription = "Trend direction",
                                    tint = trendColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (trendDirection.second == "Flat") "Flat" else String.format(Locale.getDefault(), "%+.1f%%", trendDirection.first),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = trendColor
                                )
                            }
                        }

                        ConsistencyTrendChart(
                            dailyCompletionRates = overallDailyCompletionRates,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }
            }

            // Per-Habit Consistency Cards (Vertical List for readability)
            Text(
                text = "Habit Performance",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (habitCardData.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No template habits defined yet. Add some in the Template tab!",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    habitCardData.forEach { habitItem ->
                        val taskColor = Color(android.graphics.Color.parseColor(habitItem.template.colorHex))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Accent left border
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(130.dp)
                                        .background(taskColor)
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = habitItem.template.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${habitItem.consistencyPercent}% consistency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = taskColor
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "Streak", fontSize = 9.sp, color = Color.Gray)
                                            Text(
                                                text = "${habitItem.currentStreak}d (max ${habitItem.maxStreak}d)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Column {
                                            Text(text = "Avg Duration", fontSize = 9.sp, color = Color.Gray)
                                            Text(
                                                text = "${habitItem.avgActualMinutes}m / ${habitItem.plannedMinutes}m",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.width(120.dp)
                                        ) {
                                            Text(text = "History (4w)", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                                            MiniBarChart(
                                                completions = habitItem.completions28,
                                                color = taskColor,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2D Heatmap Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Habit Matrix",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily habit completions for the past 30 days",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    if (templateTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No template activities to grid.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        val rowScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rowScrollState)
                                .padding(vertical = 4.dp)
                        ) {
                            HabitGrid(
                                templates = templateTasks,
                                past30Days = past30Days,
                                allTasks = allTasks
                            )
                        }
                    }

                    // Grid Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Skipped ", fontSize = 9.sp, color = Color.Gray)
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Partial ", fontSize = 9.sp, color = Color.Gray)
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "100% ", fontSize = 9.sp, color = Color.Gray)
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }

            // Top Habits Ranked List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Leaderboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your most consistent habits for this month",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    if (rankedHabitList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No template activities to rank.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        TopHabitsList(rankedHabits = rankedHabitList)
                    }
                }
            }
        }
    }
}

@Composable
fun ConsistencyTrendChart(
    dailyCompletionRates: List<Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val areaColor = primaryColor.copy(alpha = 0.15f)

    Canvas(modifier = modifier) {
        if (dailyCompletionRates.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        val stepX = width / (dailyCompletionRates.size - 1).coerceAtLeast(1)
        val points = dailyCompletionRates.mapIndexed { index, rate ->
            val x = index * stepX
            val y = height - (rate * (height - 10.dp.toPx())) - 5.dp.toPx() // clamp slightly within canvas bounds
            androidx.compose.ui.geometry.Offset(x, y)
        }

        // Draw light grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (height / gridLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.2f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Smooth cubic path
        val linePath = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX = (p0.x + p1.x) / 2
                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }
            }
        }

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        // Fill area gradient
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(areaColor, Color.Transparent)
            )
        )

        // Draw line stroke
        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

@Composable
fun MonthlyOverviewDonutChart(
    averageCompletionRate: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizeMin = size.minDimension
            val strokeWidth = 12.dp.toPx()
            val radius = (sizeMin - strokeWidth) / 2
            val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

            // Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeWidth)
            )

            // Swept completion arc
            if (averageCompletionRate > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = averageCompletionRate * 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(averageCompletionRate * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Done",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun MiniBarChart(
    completions: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = completions.size
        val gap = 1.5.dp.toPx()
        val totalGaps = gap * (barCount - 1)
        val barWidth = (width - totalGaps) / barCount

        completions.forEachIndexed { index, rate ->
            val barHeight = (rate * height).coerceAtLeast(1.dp.toPx())
            val x = index * (barWidth + gap)
            val y = height - barHeight

            drawRoundRect(
                color = if (rate > 0f) color else Color.LightGray.copy(alpha = 0.25f),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
fun HabitGrid(
    templates: List<TemplateTask>,
    past30Days: List<String>,
    allTasks: List<Task>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        templates.forEach { template ->
            val color = Color(android.graphics.Color.parseColor(template.colorHex))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(80.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    past30Days.forEach { dateStr ->
                        val dayTasks = allTasks.filter { it.date == dateStr }
                        val completion = calculateTaskCompletion(template.id, dayTasks)

                        val cellColor = when {
                            completion <= 0f -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            completion < 1.0f -> color.copy(alpha = 0.4f)
                            else -> color
                        }

                        val borderModifier = if (completion <= 0f) {
                            Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(cellColor)
                                .then(borderModifier)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopHabitsList(
    rankedHabits: List<RankedHabitItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rankedHabits.forEachIndexed { index, item ->
            val color = Color(android.graphics.Color.parseColor(item.colorHex))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${index + 1}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${(item.consistency * 100).toInt()}% consistency",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Data structures
data class HabitCardItem(
    val template: TemplateTask,
    val currentStreak: Int,
    val maxStreak: Int,
    val consistencyPercent: Int,
    val avgActualMinutes: Int,
    val plannedMinutes: Int,
    val completions28: List<Float>
)

data class RankedHabitItem(
    val name: String,
    val colorHex: String,
    val consistency: Float
)

// Helper methods for calculation
private fun calculateTaskCompletion(templateId: Int, dayTasks: List<Task>): Float {
    val task = dayTasks.find { it.templateTaskId == templateId } ?: return 0f
    if (!task.isCompleted) return 0f
    if (task.plannedDurationMinutes <= 0) return 0f
    return (task.actualDurationMinutes.toFloat() / task.plannedDurationMinutes.toFloat()).coerceIn(0f, 1f)
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

private fun calculateHabitStreaks(completions: List<Float>): Pair<Int, Int> {
    var currentStreak = 0
    var maxStreak = 0
    var tempStreak = 0

    completions.forEach { completion ->
        if (completion > 0f) {
            tempStreak++
            maxStreak = maxOf(maxStreak, tempStreak)
        } else {
            tempStreak = 0
        }
    }

    val todayIdx = completions.size - 1
    val yesterdayIdx = completions.size - 2

    if (todayIdx >= 0 && completions[todayIdx] > 0f) {
        currentStreak = 1
        var idx = todayIdx - 1
        while (idx >= 0 && completions[idx] > 0f) {
            currentStreak++
            idx--
        }
    } else if (yesterdayIdx >= 0 && completions[yesterdayIdx] > 0f) {
        currentStreak = 1
        var idx = yesterdayIdx - 1
        while (idx >= 0 && completions[idx] > 0f) {
            currentStreak++
            idx--
        }
    } else {
        currentStreak = 0
    }

    return Pair(currentStreak, maxStreak)
}
