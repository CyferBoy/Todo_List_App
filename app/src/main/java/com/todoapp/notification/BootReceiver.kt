package com.todoapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.todoapp.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rescheduleReminders(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun rescheduleReminders(context: Context) {
        val repository = AppModule.provideTaskRepository()
        val tasks = repository.getAllScheduledReminders()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        tasks.forEach { task ->
            if (task.isReminderEnabled && task.reminderDate != null && task.reminderTime != null) {
                val reminderDateTime = task.reminderDate.atTime(task.reminderTime)
                val triggerTime = reminderDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                if (triggerTime > System.currentTimeMillis()) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("task_id", task.id)
                        putExtra("task_title", task.title)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, task.id.hashCode(), intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } catch (_: SecurityException) {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                }
            }
        }
    }
}
