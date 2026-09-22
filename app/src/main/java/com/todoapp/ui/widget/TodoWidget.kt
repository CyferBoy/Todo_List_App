package com.todoapp.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.todoapp.MainActivity
import com.todoapp.di.AppModule
import com.todoapp.domain.model.Task
import com.todoapp.domain.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class TodoWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = AppModule.provideTaskRepository()
        val listRepository = AppModule.provideListRepository()
        val today = LocalDate.now()

        val publicList = listRepository.getPublicList()
        val listId = publicList?.id
        val listName = publicList?.name ?: "Public"

        val tasks = if (listId != null) {
            repository.getTasksByListId(listId).first().filter { it.dueDate == today }
        } else {
            repository.getTasksByDate(today).first()
        }
        val completed = tasks.count { it.isCompleted }
        val total = tasks.size

        provideContent {
            WidgetContent(tasks = tasks, total = total, completed = completed, listName = listName)
        }
    }
}

@Composable
private fun WidgetContent(tasks: List<Task>, total: Int, completed: Int, listName: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(android.R.color.white)
            .padding(12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Tasks",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Box(
                    modifier = GlanceModifier
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(4.dp)
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Text(
                text = listName,
                style = TextStyle(
                    fontSize = 11.sp
                )
            )

            Text(
                text = "Today · ${DateUtils.formatShortDate(LocalDate.now())}",
                style = TextStyle(
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            val displayTasks = tasks.take(3)
            displayTasks.forEach { task ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckBox(
                        checked = task.isCompleted,
                        onCheckedChange = actionRunCallback<ToggleTaskCallback>(
                            parameters = actionParametersOf(
                                ActionParameters.Key<String>("task_id") to task.id
                            )
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = task.title,
                        style = TextStyle(
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }

            if (total > 3) {
                Text(
                    text = "... and ${total - 3} more",
                    style = TextStyle(fontSize = 11.sp)
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "${total - completed} remaining",
                style = TextStyle(fontSize = 12.sp)
            )
        }
    }
}

class ToggleTaskCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[ActionParameters.Key<String>("task_id")] ?: return
        val repository = AppModule.provideTaskRepository()
        val task = repository.getTaskById(taskId) ?: return
        repository.updateTaskCompletion(taskId, !task.isCompleted)
        TodoWidget().updateAll(context)
    }
}
