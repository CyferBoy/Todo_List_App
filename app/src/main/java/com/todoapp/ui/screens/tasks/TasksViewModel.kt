package com.todoapp.ui.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.di.AppModule
import com.todoapp.domain.model.Category
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TaskFilter
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.usecase.category.GetCategoriesUseCase
import com.todoapp.domain.usecase.list.GetListsUseCase
import com.todoapp.domain.usecase.task.DeleteTaskUseCase
import com.todoapp.domain.usecase.task.GetAllTasksUseCase
import com.todoapp.domain.usecase.task.ToggleTaskCompletionUseCase
import com.todoapp.domain.usecase.task.UpdateTaskUseCase
import com.todoapp.notification.NotificationHelper
import com.todoapp.ui.widget.WidgetHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TasksUiState(
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val lists: List<TodoList> = emptyList(),
    val selectedListId: String? = null,
    val searchQuery: String = "",
    val currentFilter: TaskFilter = TaskFilter.ALL,
    val selectedPriority: Priority? = null,
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = true,
    val undoMessage: String? = null,
    val lastDeletedTask: Task? = null
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val getAllTasks = AppModule.provideGetAllTasksUseCase()
    private val toggleCompletion = AppModule.provideToggleTaskCompletionUseCase()
    private val deleteTask = AppModule.provideDeleteTaskUseCase()
    private val updateTask = AppModule.provideUpdateTaskUseCase()
    private val getCategories = AppModule.provideGetCategoriesUseCase()
    private val getLists = AppModule.provideGetListsUseCase()
    private val syncEngine = AppModule.provideSyncEngine()

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAllTasks().collect { tasks ->
                _uiState.value = _uiState.value.copy(allTasks = tasks, isLoading = false)
                applyFilters()
            }
        }
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

    fun onListSelected(listId: String?) {
        _uiState.value = _uiState.value.copy(selectedListId = listId)
        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onFilterChange(filter: TaskFilter) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
        applyFilters()
    }

    fun onPriorityFilterChange(priority: Priority?) {
        _uiState.value = _uiState.value.copy(selectedPriority = priority)
        applyFilters()
    }

    fun onCategoryFilterChange(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val today = LocalDate.now()
        val filtered = state.allTasks
            .asSequence()
            .filter { state.selectedListId == null || it.listId == state.selectedListId }
            .filter { state.searchQuery.isBlank() || it.title.contains(state.searchQuery, ignoreCase = true) || it.description.contains(state.searchQuery, ignoreCase = true) }
            .filter { task ->
                when (state.currentFilter) {
                    TaskFilter.ALL -> true
                    TaskFilter.TODAY -> task.dueDate == today
                    TaskFilter.UPCOMING -> task.dueDate != null && task.dueDate > today
                    TaskFilter.OVERDUE -> task.dueDate != null && task.dueDate < today && !task.isCompleted
                    TaskFilter.COMPLETED -> task.isCompleted
                    TaskFilter.PINNED -> task.isPinned
                }
            }
            .filter { state.selectedPriority == null || it.priority == state.selectedPriority }
            .filter { state.selectedCategoryId == null || it.categoryId == state.selectedCategoryId }
            .sortedWith(compareBy<Task> { it.isPinned }.thenBy { it.position }.thenBy { it.dueDate })
            .toList()
        _uiState.value = _uiState.value.copy(filteredTasks = filtered)
    }

    fun toggleTaskCompletion(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            toggleCompletion(taskId, completed)
            WidgetHelper.refreshWidget(getApplication())
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            NotificationHelper.cancelReminder(getApplication(), task.id)
            deleteTask(task)
            _uiState.value = _uiState.value.copy(undoMessage = "Task deleted", lastDeletedTask = task)
            WidgetHelper.refreshWidget(getApplication())
        }
    }

    fun undoDelete() {
        val task = _uiState.value.lastDeletedTask ?: return
        viewModelScope.launch {
            syncEngine.cancelPendingDeletion(task.id)
            updateTask(task)
            syncEngine.enqueueTaskOperation(task.id, "INSERT")
            _uiState.value = _uiState.value.copy(undoMessage = null, lastDeletedTask = null)
            WidgetHelper.refreshWidget(getApplication())
        }
    }

    fun clearUndoMessage() {
        _uiState.value = _uiState.value.copy(undoMessage = null, lastDeletedTask = null)
    }
}
