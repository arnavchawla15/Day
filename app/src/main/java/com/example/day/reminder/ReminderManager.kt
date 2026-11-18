package com.example.day.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.day.data.AppDatabase
import com.example.day.data.Task
import com.example.day.data.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderManager {

    const val CHANNEL_ID = "day_reminders_channel"
    private const val CHANNEL_NAME = "Day Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val existing = notificationManager?.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for scheduled tasks and due to-dos in Day"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    fun isRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("day_app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("reminders_enabled", true)
    }

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("day_app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
        if (enabled) {
            rescheduleAllReminders(context)
        } else {
            cancelAllReminders(context)
        }
    }

    fun scheduleTaskReminder(context: Context, task: Task) {
        if (!isRemindersEnabled(context)) return
        if (task.isCompleted) return

        val triggerTime = computeTriggerTimeMillis(task.date, task.startTime) ?: return
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, 100000 + task.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, "⏰ Task Starting: ${task.name}")
            putExtra(ReminderReceiver.EXTRA_MESSAGE, "Scheduled for ${formatTo12Hour(task.startTime)}")
            putExtra(ReminderReceiver.EXTRA_TYPE, "TASK")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100000 + task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(alarmManager, triggerTime, pendingIntent)
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100000 + taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleTodoReminder(context: Context, todo: Todo) {
        if (!isRemindersEnabled(context)) return
        if (todo.isCompleted) return
        val dueDate = todo.dueDate ?: return
        val dueTime = todo.dueTime ?: "09:00"

        val triggerTime = computeTriggerTimeMillis(dueDate, dueTime) ?: return
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, 200000 + todo.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, "📋 To-Do Reminder: ${todo.title}")
            putExtra(ReminderReceiver.EXTRA_MESSAGE, if (!todo.dueTime.isNullOrBlank()) "Due at ${formatTo12Hour(todo.dueTime)}" else "Due today")
            putExtra(ReminderReceiver.EXTRA_TYPE, "TODO")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            200000 + todo.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(alarmManager, triggerTime, pendingIntent)
    }

    fun cancelTodoReminder(context: Context, todoId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            200000 + todoId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllReminders(context: Context) {
        if (!isRemindersEnabled(context)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

                // 1. Tasks scheduled for today or future
                val tasks = db.taskDao().getAllTasksSync().filter {
                    !it.isCompleted && it.date >= todayStr
                }
                for (task in tasks) {
                    scheduleTaskReminder(context, task)
                }

                // 2. Todos due today or future with dueTime
                val todos = db.todoDao().getActiveTodosSync().filter {
                    val d = it.dueDate
                    d != null && d >= todayStr && !it.dueTime.isNullOrBlank()
                }
                for (todo in todos) {
                    scheduleTodoReminder(context, todo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelAllReminders(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val tasks = db.taskDao().getAllTasksSync()
                for (task in tasks) {
                    cancelTaskReminder(context, task.id)
                }
                val todos = db.todoDao().getAllTodos()
                // cancel all
            } catch (_: Exception) {}
        }
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun computeTriggerTimeMillis(dateStr: String, timeStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = sdf.parse("$dateStr $timeStr")
            date?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun formatTo12Hour(timeStr: String): String {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val amPm = if (h >= 12) "PM" else "AM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
        } catch (_: Exception) {
            timeStr
        }
    }
}
