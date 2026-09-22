package com.todoapp.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.di.AppModule
import com.todoapp.domain.model.Category
import com.todoapp.domain.model.ThemeMode
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.usecase.backup.ExportTasksUseCase
import com.todoapp.domain.usecase.backup.ImportTasksUseCase
import com.todoapp.domain.usecase.category.CreateCategoryUseCase
import com.todoapp.domain.usecase.category.DeleteCategoryUseCase
import com.todoapp.domain.usecase.category.GetCategoriesUseCase
import com.todoapp.domain.usecase.list.CreateListUseCase
import com.todoapp.domain.usecase.list.DeleteListUseCase
import com.todoapp.domain.usecase.list.GetListsUseCase
import com.todoapp.domain.usecase.preferences.GetThemeUseCase
import com.todoapp.domain.usecase.preferences.SetThemeUseCase
import com.todoapp.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncStatusDisplay { IDLE, SYNCING, SUCCESS, FAILED }

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val categories: List<Category> = emptyList(),
    val lists: List<TodoList> = emptyList(),
    val message: String? = null,
    val syncStatus: SyncStatusDisplay = SyncStatusDisplay.IDLE,
    val lastSyncTime: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val getTheme = AppModule.provideGetThemeUseCase()
    private val setTheme = AppModule.provideSetThemeUseCase()
    private val getCategories = AppModule.provideGetCategoriesUseCase()
    private val createCategory = AppModule.provideCreateCategoryUseCase()
    private val deleteCategory = AppModule.provideDeleteCategoryUseCase()
    private val getLists = AppModule.provideGetListsUseCase()
    private val createList = AppModule.provideCreateListUseCase()
    private val deleteList = AppModule.provideDeleteListUseCase()
    private val exportTasks = AppModule.provideExportTasksUseCase()
    private val importTasks = AppModule.provideImportTasksUseCase()
    private val syncEngine = AppModule.provideSyncEngine()
    private val taskRepository = AppModule.provideTaskRepository()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { getTheme().collect { mode -> _uiState.value = _uiState.value.copy(themeMode = mode) } }
        viewModelScope.launch { getCategories().collect { cats -> _uiState.value = _uiState.value.copy(categories = cats) } }
        viewModelScope.launch { getLists().collect { lists -> _uiState.value = _uiState.value.copy(lists = lists) } }
    }

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { setTheme(mode) } }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { createCategory(name) }
    }

    fun deleteCategory(categoryId: String) { viewModelScope.launch { deleteCategory(categoryId) } }

    fun addList(name: String, userId: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { createList(name, userId) }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            val taskIds = taskRepository.getListTaskIds(listId)
            for (taskId in taskIds) {
                NotificationHelper.cancelReminder(getApplication(), taskId)
            }
            deleteList(listId)
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val success = exportTasks(uri)
            _uiState.value = _uiState.value.copy(message = if (success) "Backup exported successfully" else "Couldn't export backup. Try again.")
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val result = importTasks(uri)
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Backup imported successfully" else "Couldn't import backup. Your data is safe. Try again."
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatusDisplay.SYNCING)
            val result = syncEngine.syncAll()
            val time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            _uiState.value = _uiState.value.copy(
                syncStatus = when (result) {
                    com.todoapp.data.sync.SyncStatus.SYNCED -> SyncStatusDisplay.SUCCESS
                    com.todoapp.data.sync.SyncStatus.SYNC_FAILED -> SyncStatusDisplay.FAILED
                    else -> SyncStatusDisplay.IDLE
                },
                lastSyncTime = time
            )
        }
    }

    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
