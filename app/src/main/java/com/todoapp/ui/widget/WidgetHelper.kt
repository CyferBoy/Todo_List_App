package com.todoapp.ui.widget

import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName

object WidgetHelper {
    fun refreshWidget(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, TodoWidgetReceiver::class.java)
        val widgetIds = manager.getAppWidgetIds(componentName)
        val intent = Intent(context, TodoWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        context.sendBroadcast(intent)
    }
}
