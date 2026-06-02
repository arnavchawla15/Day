package com.example.day.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.White))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Title
                Text(
                    text = "Today's Schedule",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ColorProvider(Color.Black)
                    )
                )

                // Quote section
                if (quote.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "\"$quote\"",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color.Gray)
                        ),
                        maxLines = 2
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (tasks.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks scheduled.",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(Color.DarkGray)
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    ) {
                        items(tasks) { task ->
                            val taskColor = try {
                                // Extract pastel color and make it slightly translucent for widget
                                Color(android.graphics.Color.parseColor(task.colorHex))
                            } catch (e: Exception) {
                                Color(0xFFE3F2FD) // Default soft blue
                            }

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(ColorProvider(taskColor))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CheckBox(
                                    checked = task.isCompleted,
                                    onCheckedChange = actionRunCallback<ToggleTaskAction>(
                                        actionParametersOf(ToggleTaskAction.TaskIdKey to task.id)
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Column {
                                    Text(
                                        text = task.name,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ColorProvider(Color.DarkGray)
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${task.startTime} - ${task.endTime}",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = ColorProvider(Color.DarkGray.copy(alpha = 0.8f))
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
