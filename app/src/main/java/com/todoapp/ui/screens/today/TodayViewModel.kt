package com.todoapp.ui.screens.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.di.AppModule
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.usecase.list.GetListsUseCase
import com.todoapp.domain.usecase.task.DeleteTaskUseCase
import com.todoapp.domain.usecase.task.GetOverdueTasksUseCase
import com.todoapp.domain.usecase.task.GetTodayTasksUseCase
import com.todoapp.domain.usecase.task.ToggleTaskCompletionUseCase
import com.todoapp.domain.usecase.task.UpdateTaskUseCase
import com.todoapp.domain.util.DateUtils
import com.todoapp.notification.NotificationHelper
import com.todoapp.ui.widget.WidgetHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class TodayUiState(
    val greeting: String = DateUtils.getGreeting(),
    val date: String = DateUtils.formatFullDate(java.time.LocalDate.now()),
    val todayTasks: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val lists: List<TodoList> = emptyList(),
    val selectedListId: String? = null,
    val selectedListName: String = "All Lists",
    val isLoading: Boolean = true,
    val undoMessage: String? = null,
    val lastDeletedTask: Task? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(application: Application) : AndroidViewModel(application) {
    private val getTodayTasks = AppModule.provideGetTodayTasksUseCase()
    private val getOverdueTasks = AppModule.provideGetOverdueTasksUseCase()
    private val toggleCompletion = AppModule.provideToggleTaskCompletionUseCase()
    private val deleteTask = AppModule.provideDeleteTaskUseCase()
    private val updateTask = AppModule.provideUpdateTaskUseCase()
    private val getLists = AppModule.provideGetListsUseCase()
    private val syncEngine = AppModule.provideSyncEngine()

    private val _selectedListId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getLists().collect { lists ->
                _uiState.value = _uiState.value.copy(lists = lists)
                updateSelectedListName(lists, _selectedListId.value)
            }
        }

        viewModelScope.launch {
            _selectedListId.flatMapLatest { listId ->
                combine(
                    getTodayTasks(),
                    getOverdueTasks()
                ) { today, overdue ->
                    val filteredToday = if (listId == null) today else today.filter { it.listId == listId }
                    val filteredOverdue = if (listId == null) overdue else overdue.filter { it.listId == listId }
                    _uiState.value.copy(
                        todayTasks = filteredToday,
                        overdueTasks = filteredOverdue,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onListSelected(listId: String?) {
        _selectedListId.value = listId
        _uiState.value = _uiState.value.copy(selectedListId = listId)
        updateSelectedListName(_uiState.value.lists, listId)
    }

    private fun updateSelectedListName(lists: List<TodoList>, listId: String?) {
        val name = if (listId == null) "All Lists"
        else lists.find { it.id == listId }?.name ?: "All Lists"
        _uiState.value = _uiState.value.copy(selectedListName = name)
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
            _uiState.value = _uiState.value.copy(
                undoMessage = "Task deleted",
                lastDeletedTask = task
            )
            WidgetHelper.refreshWidget(getApplication())
        }
    }

    fun undoDelete() {
        val task = _uiState.value.lastDeletedTask ?: return
        viewModelScope.launch {
            syncEngine.cancelPendingDeletion(task.id)
            updateTask(task)
            syncEngine.enqueueTaskOperation(task.id, "INSERT")
            _uiState.value = _uiState.value.copy(
                undoMessage = null,
                lastDeletedTask = null
            )
            WidgetHelper.refreshWidget(getApplication())
        }
    }

    fun clearUndoMessage() {
        _uiState.value = _uiState.value.copy(undoMessage = null, lastDeletedTask = null)
    }
}
