package com.example.day.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.text.FontStyle
import androidx.glance.text.TextDecoration
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import com.example.day.data.AppDatabase
import com.example.day.data.QuoteManager
import java.text.SimpleDateFormat
import java.util.*

class DayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()
        val quoteManager = QuoteManager(context)

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val tasks = taskDao.getTasksForDateSync(todayStr)
        val quote = quoteManager.getCurrentQuote()

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFFAFAFA))) // Off-white background
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color.White)) // Card body
                        .cornerRadius(16.dp)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Title Header
                    Text(
                        text = "Today's Schedule",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ColorProvider(Color(0xFF2C2C2C))
                        )
                    )

                    // Italic quote section
                    if (quote.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(Color(0xFFF5F5F5)))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\"$quote\"",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = ColorProvider(Color(0xFF616161)),
                                    fontStyle = FontStyle.Italic
                                ),
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    if (tasks.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All tasks completed!",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = ColorProvider(Color(0xFF9E9E9E))
                                )
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                        ) {
                            items(tasks) { task ->
                                val taskColor = try {
                                    Color(android.graphics.Color.parseColor(task.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFFE3F2FD) // Default soft blue
                                }

                                // 8% tint of color for row background
                                val bgTint = taskColor.copy(alpha = 0.08f)

                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(ColorProvider(bgTint))
                                        .cornerRadius(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 4dp vertical colored block indicator on the left
                                    Box(
                                        modifier = GlanceModifier
                                            .width(4.dp)
                                            .height(36.dp)
                                            .background(ColorProvider(taskColor))
                                    ) {}

                                    Spacer(modifier = GlanceModifier.width(8.dp))

                                    CheckBox(
                                        checked = task.isCompleted,
                                        onCheckedChange = actionRunCallback<ToggleTaskAction>(
                                            actionParametersOf(ToggleTaskAction.TaskIdKey to task.id)
                                        )
                                    )

                                    Spacer(modifier = GlanceModifier.width(8.dp))

                                    Column(
                                        modifier = GlanceModifier.defaultWeight()
                                    ) {
                                        Text(
                                            text = task.name,
                                            style = TextStyle(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = ColorProvider(if (task.isCompleted) Color(0xFF9E9E9E) else Color(0xFF2C2C2C)),
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${task.startTime} - ${task.endTime}",
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                color = ColorProvider(Color(0xFF757575))
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()
        val allTasks = taskDao.getAllTasksSync()
        val task = allTasks.find { it.id == taskId } ?: return
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        taskDao.updateTask(updatedTask)
        
        // Refresh the widget instance
        DayWidget().update(context, glanceId)
    }

    companion object {
        val TaskIdKey = ActionParameters.Key<Int>("task_id")
    }
}
