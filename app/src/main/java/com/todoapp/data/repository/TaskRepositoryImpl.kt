package com.todoapp.data.repository

import androidx.room.withTransaction
import com.todoapp.data.local.dao.TaskDao
import com.todoapp.data.local.entity.TaskEntity
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.TaskRepository
import com.todoapp.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val categoryDao: com.todoapp.data.local.dao.CategoryDao,
    private val database: com.todoapp.data.local.database.TodoDatabase,
    private val deletedTaskDao: com.todoapp.data.local.dao.DeletedTaskDao,
    private val syncOperationDao: com.todoapp.data.local.dao.SyncOperationDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getTasksByDate(date: LocalDate): Flow<List<Task>> {
        val epochMillis = DateUtils.localDateToEpochMillis(date)
        return taskDao.getTasksByDate(epochMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveTasksByDate(date: LocalDate): Flow<List<Task>> {
        val epochMillis = DateUtils.localDateToEpochMillis(date)
        return taskDao.getActiveTasksByDate(epochMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOverdueTasks(): Flow<List<Task>> {
        val todayMillis = DateUtils.todayEpochMillis()
        return taskDao.getOverdueTasks(todayMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksBetweenDates(start: LocalDate, end: LocalDate): Flow<List<Task>> {
        val startMillis = DateUtils.localDateToEpochMillis(start)
        val endMillis = DateUtils.localDateToEpochMillis(end)
        return taskDao.getTasksBetweenDates(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getTasksByCategory(categoryId: String): Flow<List<Task>> =
        taskDao.getTasksByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTasksByPriority(priority: String): Flow<List<Task>> =
        taskDao.getTasksByPriority(priority).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getPinnedTasks(): Flow<List<Task>> = taskDao.getPinnedTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getActiveTaskCountForDate(date: LocalDate): Flow<Int> {
        val epochMillis = DateUtils.localDateToEpochMillis(date)
        return taskDao.getActiveTaskCountForDate(epochMillis)
    }

    override suspend fun getTaskById(taskId: String): Task? = taskDao.getTaskById(taskId)?.toDomain()

    override fun getTaskByIdFlow(taskId: String): Flow<Task?> = taskDao.getTaskByIdFlow(taskId).map { it?.toDomain() }

    override suspend fun getAllScheduledReminders(): List<Task> =
        taskDao.getAllScheduledReminders().map { it.toDomain() }

    override suspend fun insertTask(task: Task) = taskDao.insertTask(task.toEntity())

    override suspend fun updateTask(task: Task) = taskDao.updateTask(task.toEntity())

    override suspend fun deleteTask(task: Task) = taskDao.deleteTask(task.toEntity())

    override suspend fun deleteTaskById(taskId: String) = taskDao.deleteTaskById(taskId)

    override suspend fun deleteTaskWithTombstone(taskId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            deletedTaskDao.insertDeletedTask(
                com.todoapp.data.local.entity.DeletedTaskEntity(id = taskId, deletedAt = now)
            )
            syncOperationDao.enqueueTaskOperation(taskId, "DELETE")
            taskDao.deleteTaskById(taskId)
        }
    }

    override suspend fun getListTaskIds(listId: String): List<String> = taskDao.getTaskIdsByListId(listId)

    override suspend fun updateTaskCompletion(taskId: String, completed: Boolean) =
        taskDao.updateTaskCompletion(taskId, completed, System.currentTimeMillis())

    override suspend fun updateTaskPin(taskId: String, pinned: Boolean) =
        taskDao.updateTaskPin(taskId, pinned, System.currentTimeMillis())

    override suspend fun updateTaskPosition(taskId: String, position: Int) =
        taskDao.updateTaskPosition(taskId, position)

    override suspend fun getNextPosition(): Int = (taskDao.getMaxPosition() ?: -1) + 1

    override fun getTasksByListId(listId: String): Flow<List<Task>> =
        taskDao.getTasksByListId(listId).map { entities -> entities.map { it.toDomain() } }

    override fun getActiveTasksByListId(listId: String): Flow<List<Task>> =
        taskDao.getActiveTasksByListId(listId).map { entities -> entities.map { it.toDomain() } }

    override fun getOverdueTasksByListId(listId: String): Flow<List<Task>> {
        val todayMillis = DateUtils.todayEpochMillis()
        return taskDao.getOverdueTasksByListId(listId, todayMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchTasksByListId(listId: String, query: String): Flow<List<Task>> =
        taskDao.searchTasksByListId(listId, query).map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateTaskList(taskId: String, listId: String) =
        taskDao.updateTaskList(taskId, listId, System.currentTimeMillis())

    private var categoryCache: Map<String, Pair<String, Long>> = emptyMap()

    private fun loadCategoryCache() {
        if (categoryCache.isEmpty()) {
            val cats = runBlocking { categoryDao.getAllCategoriesList() }
            categoryCache = cats.associate { it.id to (it.name to it.color) }
        }
    }

    private fun TaskEntity.toDomain(): Task {
        loadCategoryCache()
        val (catName, catColor) = categoryId?.let { categoryCache[it] } ?: (null to null)
        return Task(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            priority = priority,
            categoryId = categoryId,
            listId = listId,
            dueDate = dueDate?.let { DateUtils.epochMillisToLocalDate(it) },
            dueTime = dueTime?.let { LocalTime.ofSecondOfDay(it) },
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPinned = isPinned,
            position = position,
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval,
            recurrenceDaysOfWeek = recurrenceDaysOfWeek.split(",").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 },
            recurrenceEndDate = recurrenceEndDate?.let { DateUtils.epochMillisToLocalDate(it) },
            reminderDate = reminderDate?.let { DateUtils.epochMillisToLocalDate(it) },
            reminderTime = reminderTime?.let { LocalTime.ofSecondOfDay(it) },
            isReminderEnabled = isReminderEnabled,
            parentTaskId = parentTaskId,
            originalDueDate = originalDueDate?.let { DateUtils.epochMillisToLocalDate(it) },
            categoryName = catName,
            categoryColor = catColor
        )
    }

    private fun Task.toEntity(): TaskEntity = TaskEntity(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = priority,
        categoryId = categoryId,
        listId = listId,
        dueDate = dueDate?.let { DateUtils.localDateToEpochMillis(it) },
        dueTime = dueTime?.toSecondOfDay()?.toLong(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        position = position,
        recurrenceType = recurrenceType,
        recurrenceInterval = recurrenceInterval,
        recurrenceDaysOfWeek = recurrenceDaysOfWeek.joinToString(","),
        recurrenceEndDate = recurrenceEndDate?.let { DateUtils.localDateToEpochMillis(it) },
        reminderDate = reminderDate?.let { DateUtils.localDateToEpochMillis(it) },
        reminderTime = reminderTime?.toSecondOfDay()?.toLong(),
        isReminderEnabled = isReminderEnabled,
        parentTaskId = parentTaskId,
        originalDueDate = originalDueDate?.let { DateUtils.localDateToEpochMillis(it) }
    )
}
