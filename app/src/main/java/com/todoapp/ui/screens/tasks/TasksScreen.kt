package com.todoapp.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TaskFilter
import com.todoapp.ui.components.EmptyState
import com.todoapp.ui.components.TaskItem
import com.todoapp.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TasksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var listDropdownExpanded by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Tasks") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(
                modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search tasks\u2026") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(Dimens.SmallGap))

                ExposedDropdownMenuBox(
                    expanded = listDropdownExpanded,
                    onExpandedChange = { listDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.lists.find { it.id == uiState.selectedListId }?.name ?: "All Lists",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = listDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = listDropdownExpanded,
                        onDismissRequest = { listDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Lists") },
                            onClick = {
                                viewModel.onListSelected(null)
                                listDropdownExpanded = false
                            }
                        )
                        uiState.lists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list.name) },
                                onClick = {
                                    viewModel.onListSelected(list.id)
                                    listDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SmallGap))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.TinyGap)
                ) {
                    TaskFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.currentFilter == filter,
                            onClick = { viewModel.onFilterChange(filter) },
                            label = {
                                Text(
                                    when (filter) {
                                        TaskFilter.ALL -> "All"
                                        TaskFilter.TODAY -> "Today"
                                        TaskFilter.UPCOMING -> "Upcoming"
                                        TaskFilter.OVERDUE -> "Overdue"
                                        TaskFilter.COMPLETED -> "Done"
                                        TaskFilter.PINNED -> "Pinned"
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.TinyGap))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.TinyGap)
                ) {
                    FilterChip(
                        selected = uiState.selectedPriority == null,
                        onClick = { viewModel.onPriorityFilterChange(null) },
                        label = { Text("All Priorities") }
                    )
                    Priority.entries.filter { it != Priority.NONE }.forEach { priority ->
                        FilterChip(
                            selected = uiState.selectedPriority == priority,
                            onClick = { viewModel.onPriorityFilterChange(priority) },
                            label = {
                                Text(priority.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.TinyGap))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.TinyGap)
                ) {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null,
                        onClick = { viewModel.onCategoryFilterChange(null) },
                        label = { Text("All Categories") }
                    )
                    uiState.categories.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = { viewModel.onCategoryFilterChange(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SmallGap))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(uiState.filteredTasks, key = { _, task -> task.id }) { index, task ->
                    TaskItem(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task.id, it) },
                        onClick = { onEditTask(task.id) },
                        onLongClick = { taskToDelete = task },
                        showDivider = index < uiState.filteredTasks.size - 1
                    )
                }

                if (uiState.filteredTasks.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState(
                            icon = {
                                Icon(
                                    Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            title = "No matching tasks",
                            description = if (uiState.searchQuery.isNotEmpty())
                                "Try a different search term"
                            else
                                "Create a task to get started"
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
