package com.todoapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.todoapp.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, position ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY isPinned DESC, position ASC, dueDate ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY isPinned DESC, position ASC, dueTime ASC")
    fun getTasksByDate(date: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isCompleted = 0 ORDER BY isPinned DESC, position ASC, dueTime ASC")
    fun getActiveTasksByDate(date: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate < :today AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getOverdueTasks(today: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate >= :start AND dueDate <= :end ORDER BY dueDate ASC, position ASC")
    fun getTasksBetweenDates(start: Long, end: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY isPinned DESC, position ASC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId ORDER BY position ASC")
    fun getTasksByCategory(categoryId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY position ASC")
    fun getTasksByPriority(priority: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isPinned = 1 ORDER BY position ASC")
    fun getPinnedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: String): Flow<TaskEntity?>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND dueDate = :date")
    fun getActiveTaskCountForDate(date: Long): Flow<Int>

    @Query("SELECT MAX(position) FROM tasks")
    suspend fun getMaxPosition(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskPin(taskId: String, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET position = :position WHERE id = :taskId")
    suspend fun updateTaskPosition(taskId: String, position: Int)

    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND isReminderEnabled = 1 AND isCompleted = 0")
    suspend fun getAllScheduledReminders(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY isPinned DESC, position ASC, dueDate ASC")
    fun getTasksByListId(listId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND isCompleted = 0 ORDER BY isPinned DESC, position ASC, dueDate ASC")
    fun getActiveTasksByListId(listId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND dueDate < :today AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getOverdueTasksByListId(listId: String, today: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND title LIKE '%' || :query || '%' OR (listId = :listId AND description LIKE '%' || :query || '%') ORDER BY isPinned DESC, position ASC")
    fun searchTasksByListId(listId: String, query: String): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET listId = :listId, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskList(taskId: String, listId: String, updatedAt: Long)

    @Query("SELECT id FROM tasks WHERE listId = :listId")
    suspend fun getTaskIdsByListId(listId: String): List<String>
}
