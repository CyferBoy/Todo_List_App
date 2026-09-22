package com.todoapp.di

import android.content.Context
import com.todoapp.data.local.database.TodoDatabase
import com.todoapp.data.repository.BackupRepositoryImpl
import com.todoapp.data.repository.CategoryRepositoryImpl
import com.todoapp.data.repository.ListRepositoryImpl
import com.todoapp.data.repository.PreferencesRepositoryImpl
import com.todoapp.data.repository.TaskRepositoryImpl
import com.todoapp.domain.repository.BackupRepository
import com.todoapp.domain.repository.CategoryRepository
import com.todoapp.domain.repository.ListRepository
import com.todoapp.domain.repository.PreferencesRepository
import com.todoapp.domain.repository.TaskRepository
import com.todoapp.domain.usecase.backup.ExportTasksUseCase
import com.todoapp.domain.usecase.backup.ImportTasksUseCase
import com.todoapp.domain.usecase.category.CreateCategoryUseCase
import com.todoapp.domain.usecase.category.DeleteCategoryUseCase
import com.todoapp.domain.usecase.category.GetCategoriesUseCase
import com.todoapp.domain.usecase.list.GetListsUseCase
import com.todoapp.domain.usecase.list.CreateListUseCase
import com.todoapp.domain.usecase.list.DeleteListUseCase
import com.todoapp.domain.usecase.preferences.GetThemeUseCase
import com.todoapp.domain.usecase.preferences.SetThemeUseCase
import com.todoapp.domain.usecase.task.CreateTaskUseCase
import com.todoapp.domain.usecase.task.DeleteTaskUseCase
import com.todoapp.domain.usecase.task.GetAllTasksUseCase
import com.todoapp.domain.usecase.task.GetOverdueTasksUseCase
import com.todoapp.domain.usecase.task.GetTasksByDateRangeUseCase
import com.todoapp.domain.usecase.task.GetTodayTasksUseCase
import com.todoapp.domain.usecase.task.ToggleTaskCompletionUseCase
import com.todoapp.domain.usecase.task.UpdateTaskUseCase
import com.todoapp.data.sync.SyncEngine

object AppModule {
    private lateinit var appContext: Context
    private var database: TodoDatabase? = null
    private var taskRepository: TaskRepository? = null
    private var categoryRepository: CategoryRepository? = null
    private var preferencesRepository: PreferencesRepository? = null
    private var backupRepository: BackupRepository? = null
    private var listRepository: ListRepository? = null
    private var syncEngine: SyncEngine? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        database = TodoDatabase.getInstance(appContext)
        taskRepository = TaskRepositoryImpl(database!!.taskDao(), database!!.categoryDao(), database!!, database!!.deletedTaskDao(), database!!.syncOperationDao())
        categoryRepository = CategoryRepositoryImpl(database!!.categoryDao())
        preferencesRepository = PreferencesRepositoryImpl(appContext)
        listRepository = ListRepositoryImpl(database!!.todoListDao(), database!!, database!!.taskDao(), database!!.deletedTaskDao(), database!!.syncOperationDao(), database!!.deletedListDao())
        backupRepository = BackupRepositoryImpl(appContext, taskRepository!!, categoryRepository!!, listRepository!!, database!!)
        syncEngine = SyncEngine(appContext, taskRepository!!, listRepository!!, database!!.syncOperationDao(), database!!.deletedTaskDao(), database!!.deletedListDao())
    }

    fun provideTaskRepository(): TaskRepository = taskRepository!!
    fun provideCategoryRepository(): CategoryRepository = categoryRepository!!
    fun providePreferencesRepository(): PreferencesRepository = preferencesRepository!!
    fun provideBackupRepository(): BackupRepository = backupRepository!!
    fun provideListRepository(): ListRepository = listRepository!!
    fun provideSyncEngine(): SyncEngine = syncEngine!!

    fun provideGetAllTasksUseCase() = GetAllTasksUseCase(taskRepository!!)
    fun provideGetTodayTasksUseCase() = GetTodayTasksUseCase(taskRepository!!)
    fun provideGetOverdueTasksUseCase() = GetOverdueTasksUseCase(taskRepository!!)
    fun provideCreateTaskUseCase() = CreateTaskUseCase(taskRepository!!)
    fun provideUpdateTaskUseCase() = UpdateTaskUseCase(taskRepository!!)
    fun provideDeleteTaskUseCase() = DeleteTaskUseCase(taskRepository!!)
    fun provideToggleTaskCompletionUseCase() = ToggleTaskCompletionUseCase(taskRepository!!, appContext, syncEngine!!)
    fun provideGetTasksByDateRangeUseCase() = GetTasksByDateRangeUseCase(taskRepository!!)

    fun provideGetCategoriesUseCase() = GetCategoriesUseCase(categoryRepository!!)
    fun provideCreateCategoryUseCase() = CreateCategoryUseCase(categoryRepository!!)
    fun provideDeleteCategoryUseCase() = DeleteCategoryUseCase(categoryRepository!!)

    fun provideGetThemeUseCase() = GetThemeUseCase(preferencesRepository!!)
    fun provideSetThemeUseCase() = SetThemeUseCase(preferencesRepository!!)

    fun provideExportTasksUseCase() = ExportTasksUseCase(backupRepository!!)
    fun provideImportTasksUseCase() = ImportTasksUseCase(backupRepository!!)

    fun provideGetListsUseCase() = GetListsUseCase(listRepository!!)
    fun provideCreateListUseCase() = CreateListUseCase(listRepository!!)
    fun provideDeleteListUseCase() = DeleteListUseCase(listRepository!!)
}
