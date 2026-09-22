package com.todoapp.ui.screens.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.di.AppModule
import com.todoapp.domain.model.Category
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.usecase.category.GetCategoriesUseCase
import com.todoapp.domain.usecase.list.GetListsUseCase
import com.todoapp.domain.usecase.task.CreateTaskUseCase
import com.todoapp.domain.usecase.task.DeleteTaskUseCase
import com.todoapp.domain.usecase.task.UpdateTaskUseCase
import com.todoapp.notification.NotificationHelper
import com.todoapp.ui.widget.WidgetHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: Priority = Priority.NONE,
    val categoryId: String? = null,
    val listId: String = TodoList.PUBLIC_LIST_ID,
    val isPinned: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: List<Int> = emptyList(),
    val recurrenceEndDate: LocalDate? = null,
    val reminderEnabled: Boolean = false,
    val reminderDate: LocalDate? = null,
    val reminderTime: LocalTime? = null,
    val categories: List<Category> = emptyList(),
    val lists: List<TodoList> = emptyList(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class AddEditTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val createTask = AppModule.provideCreateTaskUseCase()
    private val updateTask = AppModule.provideUpdateTaskUseCase()
    private val deleteTaskUseCase = AppModule.provideDeleteTaskUseCase()
    private val getCategories = AppModule.provideGetCategoriesUseCase()
    private val getLists = AppModule.provideGetListsUseCase()

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private var editingTaskId: String? = null
    private var previousReminderEnabled: Boolean = false

    init {
        viewModelScope.launch {
            getCategories().collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
        viewModelScope.launch {
            getLists().collect { lists ->
                _uiState.value = _uiState.value.copy(lists = lists)
            }
        }
    }

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            val task = AppModule.provideTaskRepository().getTaskById(taskId) ?: return@launch
            editingTaskId = task.id
            previousReminderEnabled = task.isReminderEnabled
            _uiState.value = _uiState.value.copy(
                title = task.title,
                description = task.description,
                dueDate = task.dueDate,
                dueTime = task.dueTime,
                priority = task.priority,
                categoryId = task.categoryId,
                listId = task.listId ?: TodoList.PUBLIC_LIST_ID,
                isPinned = task.isPinned,
                recurrenceType = task.recurrenceType,
                recurrenceInterval = task.recurrenceInterval,
                recurrenceDaysOfWeek = task.recurrenceDaysOfWeek,
                recurrenceEndDate = task.recurrenceEndDate,
                reminderEnabled = task.isReminderEnabled,
                reminderDate = task.reminderDate,
                reminderTime = task.reminderTime,
                isEditing = true
            )
        }
    }

    fun onTitleChange(title: String) { _uiState.value = _uiState.value.copy(title = title) }
    fun onDescriptionChange(desc: String) { _uiState.value = _uiState.value.copy(description = desc) }
    fun onDueDateChange(date: LocalDate?) { _uiState.value = _uiState.value.copy(dueDate = date) }
    fun onDueTimeChange(time: LocalTime?) { _uiState.value = _uiState.value.copy(dueTime = time) }
    fun onPriorityChange(priority: Priority) { _uiState.value = _uiState.value.copy(priority = priority) }
    fun onCategoryChange(categoryId: String?) { _uiState.value = _uiState.value.copy(categoryId = categoryId) }
    fun onPinnedChange(pinned: Boolean) { _uiState.value = _uiState.value.copy(isPinned = pinned) }
    fun onRecurrenceTypeChange(type: RecurrenceType) { _uiState.value = _uiState.value.copy(recurrenceType = type) }
    fun onRecurrenceIntervalChange(interval: Int) { _uiState.value = _uiState.value.copy(recurrenceInterval = interval) }
    fun onRecurrenceDaysOfWeekChange(days: List<Int>) { _uiState.value = _uiState.value.copy(recurrenceDaysOfWeek = days) }
    fun onRecurrenceEndDateChange(date: LocalDate?) { _uiState.value = _uiState.value.copy(recurrenceEndDate = date) }
    fun onReminderEnabledChange(enabled: Boolean) { _uiState.value = _uiState.value.copy(reminderEnabled = enabled) }
    fun onReminderDateChange(date: LocalDate?) { _uiState.value = _uiState.value.copy(reminderDate = date) }
    fun onReminderTimeChange(time: LocalTime?) { _uiState.value = _uiState.value.copy(reminderTime = time) }
    fun onListChange(listId: String) { _uiState.value = _uiState.value.copy(listId = listId) }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Title is required")
            return
        }

        viewModelScope.launch {
            val task = Task(
                id = editingTaskId ?: "",
                title = state.title.trim(),
                description = state.description.trim(),
                dueDate = state.dueDate,
                dueTime = state.dueTime,
                priority = state.priority,
                categoryId = state.categoryId,
                listId = state.listId,
                isPinned = state.isPinned,
                recurrenceType = state.recurrenceType,
                recurrenceInterval = state.recurrenceInterval,
                recurrenceDaysOfWeek = state.recurrenceDaysOfWeek,
                recurrenceEndDate = state.recurrenceEndDate,
                isReminderEnabled = state.reminderEnabled,
                reminderDate = state.reminderDate,
                reminderTime = state.reminderTime
            )

            val context = getApplication<Application>()

            if (state.isEditing) {
                updateTask(task)
                if (previousReminderEnabled) {
                    NotificationHelper.cancelReminder(context, task.id)
                }
            } else {
                val savedTask = createTask(task)
                if (state.reminderEnabled) {
                    NotificationHelper.scheduleReminder(context, savedTask)
                }
                WidgetHelper.refreshWidget(context)
                _uiState.value = state.copy(isSaved = true)
                return@launch
            }

            if (state.reminderEnabled) {
                NotificationHelper.scheduleReminder(context, task)
            }

            WidgetHelper.refreshWidget(context)
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun deleteTask() {
        val taskId = editingTaskId ?: return
        viewModelScope.launch {
            val task = AppModule.provideTaskRepository().getTaskById(taskId) ?: return@launch
            deleteTaskUseCase(task)
            val context = getApplication<Application>()
            NotificationHelper.cancelReminder(context, taskId)
            WidgetHelper.refreshWidget(context)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
