package com.todoapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.todoapp.data.remote.SupabaseAuthService
import com.todoapp.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppModule.init(this)
        createNotificationChannel()
        applicationScope.launch {
            val userId = SupabaseAuthService.ensureSession(this@TodoApplication)
            if (userId != null) {
                val repository = AppModule.provideListRepository()
                val lists = repository.getAllListsList()
                lists.filter { it.type == com.todoapp.domain.model.ListType.PRIVATE && it.ownerId == null }
                    .forEach { list ->
                        repository.updateListOwner(list.id, userId)
                    }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "task_reminders",
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
