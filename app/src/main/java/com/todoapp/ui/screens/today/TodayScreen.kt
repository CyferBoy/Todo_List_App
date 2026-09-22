package com.todoapp.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.domain.model.Task
import com.todoapp.ui.components.EmptyState
import com.todoapp.ui.components.SectionHeader
import com.todoapp.ui.components.TaskItem
import com.todoapp.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TodayViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(uiState.undoMessage) {
        uiState.undoMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUndoMessage()
        }
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete task?") },
            text = { Text("Delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
                    taskToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.greeting,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.date,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            val totalTasks = uiState.overdueTasks.size + uiState.todayTasks.size
            val completedTasks = uiState.todayTasks.count { it.isCompleted } +
                    uiState.overdueTasks.count { it.isCompleted }

            if (totalTasks > 0) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$completedTasks of $totalTasks tasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (completedTasks == totalTasks && totalTasks > 0) {
                                Text(
                                    text = "All done!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Dimens.SmallGap))
                        LinearProgressIndicator(
                            progress = { if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(Dimens.SectionGap))
                    }
                }
            }

            if (uiState.overdueTasks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Overdue",
                        titleColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
                    )
                }
                itemsIndexed(uiState.overdueTasks, key = { _, task -> task.id }) { index, task ->
                    TaskItem(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task.id, it) },
                        onClick = { onEditTask(task.id) },
                        onLongClick = { taskToDelete = task },
                        showDivider = index < uiState.overdueTasks.size - 1
                    )
                }
                item { Spacer(modifier = Modifier.height(Dimens.SmallGap)) }
            }

            if (uiState.todayTasks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Today",
                        modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
                    )
                }
                itemsIndexed(uiState.todayTasks, key = { _, task -> task.id }) { index, task ->
                    TaskItem(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task.id, it) },
                        onClick = { onEditTask(task.id) },
                        onLongClick = { taskToDelete = task },
                        showDivider = index < uiState.todayTasks.size - 1
                    )
                }
            }

            if (uiState.overdueTasks.isEmpty() && uiState.todayTasks.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Outlined.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        title = "Nothing scheduled",
                        description = "Your day is clear."
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(Dimens.BottomSpacer)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
