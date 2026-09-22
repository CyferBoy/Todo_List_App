package com.todoapp.ui.screens.addedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.util.DateUtils
import com.todoapp.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskScreen(
    taskId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditTaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(taskId) {
        if (taskId != null) viewModel.loadTask(taskId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showRecurrenceEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val selectedList = uiState.lists.find { it.id == uiState.listId }
    val isPublicList = selectedList?.type == ListType.PUBLIC

    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (taskId == null) {
            titleFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Task" else "New Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveTask() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenHorizontalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.FieldGap)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Task title *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                singleLine = true,
                isError = uiState.error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Text(
                text = "List",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                uiState.lists.forEach { list ->
                    FilterChip(
                        selected = uiState.listId == list.id,
                        onClick = { viewModel.onListChange(list.id) },
                        label = { Text(list.name) }
                    )
                }
            }
            if (isPublicList) {
                Text(
                    text = "Shared list \u2014 changes are visible to everyone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Priority",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                Priority.entries.filter { it != Priority.NONE }.forEach { priority ->
                    FilterChip(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.onPriorityChange(priority) },
                        label = { Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Text(
                text = "Due Date",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                FilterChip(
                    selected = uiState.dueDate != null,
                    onClick = { showDueDatePicker = true },
                    label = {
                        Text(
                            if (uiState.dueDate != null) DateUtils.formatDate(uiState.dueDate!!)
                            else "Set date"
                        )
                    }
                )
                if (uiState.dueDate != null) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.onDueDateChange(null) },
                        label = { Text("Remove") }
                    )
                }
            }

            Text(
                text = "Due Time",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                FilterChip(
                    selected = uiState.dueTime != null,
                    onClick = { showDueTimePicker = true },
                    label = {
                        Text(
                            if (uiState.dueTime != null) DateUtils.formatTime(uiState.dueTime!!)
                            else "Set time"
                        )
                    }
                )
                if (uiState.dueTime != null) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.onDueTimeChange(null) },
                        label = { Text("Remove") }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Text(
                text = "Repeat",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                RecurrenceType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.recurrenceType == type,
                        onClick = { viewModel.onRecurrenceTypeChange(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.recurrenceType != RecurrenceType.NONE,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.FieldGap)) {
                    Text(
                        text = "Repeat every",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                        (1..30).forEach { n ->
                            FilterChip(
                                selected = uiState.recurrenceInterval == n,
                                onClick = { viewModel.onRecurrenceIntervalChange(n) },
                                label = { Text("$n") }
                            )
                        }
                    }

                    if (uiState.recurrenceType == RecurrenceType.WEEKLY) {
                        Text(
                            text = "On days",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEachIndexed { index, day ->
                                FilterChip(
                                    selected = uiState.recurrenceDaysOfWeek.contains(index),
                                    onClick = {
                                        val newDays = if (uiState.recurrenceDaysOfWeek.contains(index)) {
                                            uiState.recurrenceDaysOfWeek - index
                                        } else {
                                            uiState.recurrenceDaysOfWeek + index
                                        }
                                        viewModel.onRecurrenceDaysOfWeekChange(newDays)
                                    },
                                    label = { Text(day) }
                                )
                            }
                        }
                    }

                    Text(
                        text = "End date",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                        FilterChip(
                            selected = uiState.recurrenceEndDate != null,
                            onClick = { showRecurrenceEndDatePicker = true },
                            label = {
                                Text(
                                    if (uiState.recurrenceEndDate != null) DateUtils.formatDate(uiState.recurrenceEndDate!!)
                                    else "Set end date"
                                )
                            }
                        )
                        if (uiState.recurrenceEndDate != null) {
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.onRecurrenceEndDateChange(null) },
                                label = { Text("Remove") }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Pin task", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.isPinned,
                    onCheckedChange = { viewModel.onPinnedChange(it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Reminder", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.reminderEnabled,
                    onCheckedChange = { viewModel.onReminderEnabledChange(it) }
                )
            }

            AnimatedVisibility(
                visible = uiState.reminderEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.FieldGap)) {
                    Text(
                        text = "Reminder Date",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                        FilterChip(
                            selected = uiState.reminderDate != null,
                            onClick = { showReminderDatePicker = true },
                            label = {
                                Text(
                                    if (uiState.reminderDate != null) DateUtils.formatDate(uiState.reminderDate!!)
                                    else "Set reminder date"
                                )
                            }
                        )
                        if (uiState.reminderDate != null) {
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.onReminderDateChange(null) },
                                label = { Text("Remove") }
                            )
                        }
                    }

                    Text(
                        text = "Reminder Time",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallGap)) {
                        FilterChip(
                            selected = uiState.reminderTime != null,
                            onClick = { showReminderTimePicker = true },
                            label = {
                                Text(
                                    if (uiState.reminderTime != null) DateUtils.formatTime(uiState.reminderTime!!)
                                    else "Set reminder time"
                                )
                            }
                        )
                        if (uiState.reminderTime != null) {
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.onReminderTimeChange(null) },
                                label = { Text("Remove") }
                            )
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (uiState.isEditing) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(Dimens.SmallGap))
                    Text(
                        "Delete Task",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.BottomSpacer))
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete task?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onDueDateChange(date)
                    }
                    showDueDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDueTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.dueTime?.hour ?: 12,
            initialMinute = uiState.dueTime?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showDueTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDueTimeChange(
                        java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                    showDueTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showReminderDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onReminderDateChange(date)
                    }
                    showReminderDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.reminderTime?.hour ?: 12,
            initialMinute = uiState.reminderTime?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onReminderTimeChange(
                        java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                    showReminderTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showRecurrenceEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showRecurrenceEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onRecurrenceEndDateChange(date)
                    }
                    showRecurrenceEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecurrenceEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
