package com.todoapp.domain.repository

import com.todoapp.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getActiveTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>
    fun getActiveTasksByDate(date: LocalDate): Flow<List<Task>>
    fun getOverdueTasks(): Flow<List<Task>>
    fun getTasksBetweenDates(start: LocalDate, end: LocalDate): Flow<List<Task>>
    fun searchTasks(query: String): Flow<List<Task>>
    fun getTasksByCategory(categoryId: String): Flow<List<Task>>
    fun getTasksByPriority(priority: String): Flow<List<Task>>
    fun getPinnedTasks(): Flow<List<Task>>
    fun getActiveTaskCountForDate(date: LocalDate): Flow<Int>
    suspend fun getTaskById(taskId: String): Task?
    fun getTaskByIdFlow(taskId: String): Flow<Task?>
    suspend fun getAllScheduledReminders(): List<Task>
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun deleteTaskById(taskId: String)
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean)
    suspend fun updateTaskPin(taskId: String, pinned: Boolean)
    suspend fun updateTaskPosition(taskId: String, position: Int)
    suspend fun getNextPosition(): Int
    fun getTasksByListId(listId: String): Flow<List<Task>>
    fun getActiveTasksByListId(listId: String): Flow<List<Task>>
    fun getOverdueTasksByListId(listId: String): Flow<List<Task>>
    fun searchTasksByListId(listId: String, query: String): Flow<List<Task>>
    suspend fun updateTaskList(taskId: String, listId: String)
    suspend fun deleteTaskWithTombstone(taskId: String)
    suspend fun getListTaskIds(listId: String): List<String>
}
