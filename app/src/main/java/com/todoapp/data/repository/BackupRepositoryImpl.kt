package com.todoapp.data.repository

import android.content.Context
import android.net.Uri
import com.todoapp.data.local.database.TodoDatabase
import com.todoapp.data.remote.SupabaseAuthService
import com.todoapp.domain.model.Category
import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.repository.BackupRepository
import com.todoapp.domain.repository.CategoryRepository
import com.todoapp.domain.repository.ListRepository
import com.todoapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalTime

@Serializable
private data class SerializedTask(
    val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "NONE",
    val categoryId: String? = null,
    val listId: String? = null,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isPinned: Boolean = false,
    val position: Int = 0,
    val recurrenceType: String = "NONE",
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: String = "",
    val recurrenceEndDate: String? = null,
    val reminderDate: String? = null,
    val reminderTime: String? = null,
    val isReminderEnabled: Boolean = false,
    val parentTaskId: String? = null,
    val originalDueDate: String? = null
)

@Serializable
private data class SerializedCategory(
    val id: String,
    val name: String,
    val color: Long = 0xFF6750A4,
    val position: Int = 0,
    val createdAt: Long = 0
)

@Serializable
private data class SerializedList(
    val id: String,
    val name: String,
    val type: String = "private",
    val ownerId: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
private data class SerializedBackup(
    val version: Int = 1,
    val exportDate: Long = 0,
    val tasks: List<SerializedTask> = emptyList(),
    val categories: List<SerializedCategory> = emptyList(),
    val lists: List<SerializedList> = emptyList()
)

class BackupRepositoryImpl(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val listRepository: ListRepository,
    private val database: TodoDatabase
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportToFile(uri: Uri): Boolean {
        return try {
            val tasks = taskRepository.getAllTasks().first()
            val categories = categoryRepository.getAllCategories().first()
            val lists = listRepository.getAllLists().first()

            val backup = SerializedBackup(
                version = 1,
                exportDate = System.currentTimeMillis(),
                tasks = tasks.map { it.toSerialized() },
                categories = categories.map { it.toSerialized() },
                lists = lists.map { it.toSerialized() }
            )

            val jsonString = json.encodeToString(SerializedBackup.serializer(), backup)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun importFromFile(uri: Uri): Result<Unit> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file"))
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()

            val backup = json.decodeFromString(SerializedBackup.serializer(), jsonString)

            if (backup.version > 1) {
                return Result.failure(Exception("Unsupported backup version: ${backup.version}"))
            }

            val validationError = validateBackup(backup)
            if (validationError != null) {
                return Result.failure(Exception(validationError))
            }

            val currentUserId = SupabaseAuthService.getCurrentUserId(context)

            val hasLists = backup.lists.isNotEmpty()

            val reassignedLists = if (hasLists) {
                backup.lists.map { serialized ->
                    val list = serialized.toDomain()
                    if (list.type == ListType.PUBLIC) {
                        list.copy(
                            id = TodoList.PUBLIC_LIST_ID,
                            name = "Public",
                            type = ListType.PUBLIC,
                            ownerId = null
                        )
                    } else if (list.type == ListType.PRIVATE && currentUserId != null) {
                        list.copy(ownerId = currentUserId)
                    } else {
                        list
                    }
                }
            } else {
                val now = System.currentTimeMillis()
                val ownerId = currentUserId ?: ""
                listOf(TodoList(id = "list_personal", name = "Personal", type = ListType.PRIVATE, ownerId = ownerId, createdAt = now, updatedAt = now))
            }

            val listIdMap = reassignedLists.associateBy { it.id }
            val tasks = backup.tasks.map { serialized ->
                val task = serialized.toDomain(hasLists)
                val targetListId = task.listId?.let { id ->
                    listIdMap[id]?.id ?: task.listId
                }
                task.copy(listId = targetListId)
            }
            val categories = backup.categories.map { it.toDomain() }

            database.withTransaction {
                database.syncOperationDao().deleteAll()

                val existingLists = listRepository.getAllLists().first()
                existingLists.filter { it.id != TodoList.PUBLIC_LIST_ID }.forEach { listRepository.deleteList(it) }

                val existingTasks = taskRepository.getAllTasks().first()
                existingTasks.forEach { taskRepository.deleteTask(it) }

                val existingCategories = categoryRepository.getAllCategories().first()
                existingCategories.forEach { categoryRepository.deleteCategory(it) }

                reassignedLists.filter { it.id != TodoList.PUBLIC_LIST_ID }.forEach { listRepository.insertList(it) }
                categories.forEach { categoryRepository.insertCategory(it) }
                tasks.forEach { taskRepository.insertTask(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateBackup(backup: SerializedBackup): String? {
        if (backup.tasks.isEmpty() && backup.categories.isEmpty() && backup.lists.isEmpty()) {
            return "Backup contains no tasks, categories, or lists"
        }

        val categoryIds = backup.categories.map { it.id }.toSet()
        val listIds = backup.lists.map { it.id }.toSet()

        for (task in backup.tasks) {
            if (task.id.isBlank()) {
                return "Task has blank id"
            }
            if (task.title.isBlank()) {
                return "Task '${task.id}' has blank title"
            }
            if (task.priority !in Priority.entries.map { it.name }) {
                return "Task '${task.id}' has invalid priority: ${task.priority}"
            }
            if (task.recurrenceType !in RecurrenceType.entries.map { it.name }) {
                return "Task '${task.id}' has invalid recurrenceType: ${task.recurrenceType}"
            }
            task.dueDate?.let { parseDate(it, "Task '${task.id}' dueDate")?.let { return it } }
            task.dueTime?.let { parseTime(it, "Task '${task.id}' dueTime")?.let { return it } }
            task.recurrenceEndDate?.let { parseDate(it, "Task '${task.id}' recurrenceEndDate")?.let { return it } }
            task.reminderDate?.let { parseDate(it, "Task '${task.id}' reminderDate")?.let { return it } }
            task.reminderTime?.let { parseTime(it, "Task '${task.id}' reminderTime")?.let { return it } }
            task.originalDueDate?.let { parseDate(it, "Task '${task.id}' originalDueDate")?.let { return it } }

            task.recurrenceDaysOfWeek.split(",").filter { it.isNotEmpty() }.forEach { dayStr ->
                val day = dayStr.toIntOrNull()
                if (day == null || day !in 0..6) {
                    return "Task '${task.id}' has invalid recurrenceDaysOfWeek value: $dayStr"
                }
            }

            if (task.categoryId != null && task.categoryId !in categoryIds) {
                return "Task '${task.id}' references non-existent category: ${task.categoryId}"
            }

            if (backup.lists.isNotEmpty() && task.listId != null && task.listId !in listIds) {
                return "Task '${task.id}' references non-existent list: ${task.listId}"
            }
        }

        val taskIds = mutableSetOf<String>()
        for (task in backup.tasks) {
            if (!taskIds.add(task.id)) {
                return "Duplicate task id: ${task.id}"
            }
        }

        val categoryNames = mutableSetOf<String>()
        for (category in backup.categories) {
            if (category.id.isBlank()) {
                return "Category has blank id"
            }
            if (category.name.isBlank()) {
                return "Category '${category.id}' has blank name"
            }
            if (!categoryNames.add(category.name.lowercase())) {
                return "Duplicate category name: ${category.name}"
            }
        }

        val listIdsSet = mutableSetOf<String>()
        for (list in backup.lists) {
            if (list.id.isBlank()) {
                return "List has blank id"
            }
            if (list.name.isBlank()) {
                return "List '${list.id}' has blank name"
            }
            if (!listIdsSet.add(list.id)) {
                return "Duplicate list id: ${list.id}"
            }
        }

        return null
    }

    private fun parseDate(value: String, fieldName: String): String? {
        return try {
            LocalDate.parse(value)
            null
        } catch (_: Exception) {
            "$fieldName has invalid date format: $value"
        }
    }

    private fun parseTime(value: String, fieldName: String): String? {
        return try {
            LocalTime.parse(value)
            null
        } catch (_: Exception) {
            "$fieldName has invalid time format: $value"
        }
    }

    private fun Task.toSerialized() = SerializedTask(
        id = id, title = title, description = description, isCompleted = isCompleted,
        priority = priority.name, categoryId = categoryId, listId = listId,
        dueDate = dueDate?.toString(), dueTime = dueTime?.toString(),
        createdAt = createdAt, updatedAt = updatedAt, isPinned = isPinned, position = position,
        recurrenceType = recurrenceType.name, recurrenceInterval = recurrenceInterval,
        recurrenceDaysOfWeek = recurrenceDaysOfWeek.joinToString(","),
        recurrenceEndDate = recurrenceEndDate?.toString(),
        reminderDate = reminderDate?.toString(), reminderTime = reminderTime?.toString(),
        isReminderEnabled = isReminderEnabled, parentTaskId = parentTaskId,
        originalDueDate = originalDueDate?.toString()
    )

    private fun Category.toSerialized() = SerializedCategory(
        id = id, name = name, color = color, position = position, createdAt = createdAt
    )

    private fun TodoList.toSerialized() = SerializedList(
        id = id, name = name, type = type.name.lowercase(), ownerId = ownerId,
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun SerializedTask.toDomain(hasLists: Boolean) = Task(
        id = id, title = title, description = description, isCompleted = isCompleted,
        priority = try { Priority.valueOf(priority) } catch (_: Exception) { Priority.NONE },
        categoryId = categoryId,
        listId = if (hasLists) listId else "list_personal",
        dueDate = dueDate?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } },
        dueTime = dueTime?.let { try { LocalTime.parse(it) } catch (_: Exception) { null } },
        createdAt = createdAt, updatedAt = updatedAt, isPinned = isPinned, position = position,
        recurrenceType = try { RecurrenceType.valueOf(recurrenceType) } catch (_: Exception) { RecurrenceType.NONE },
        recurrenceInterval = recurrenceInterval,
        recurrenceDaysOfWeek = recurrenceDaysOfWeek.split(",").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 },
        recurrenceEndDate = recurrenceEndDate?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } },
        reminderDate = reminderDate?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } },
        reminderTime = reminderTime?.let { try { LocalTime.parse(it) } catch (_: Exception) { null } },
        isReminderEnabled = isReminderEnabled, parentTaskId = parentTaskId,
        originalDueDate = originalDueDate?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } }
    )

    private fun SerializedCategory.toDomain() = Category(
        id = id, name = name, color = color, position = position, createdAt = createdAt
    )

    private fun SerializedList.toDomain() = TodoList(
        id = id, name = name,
        type = when (type) {
            "public" -> ListType.PUBLIC
            else -> ListType.PRIVATE
        },
        ownerId = ownerId, createdAt = createdAt, updatedAt = updatedAt
    )
}
