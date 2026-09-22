package com.todoapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.todoapp.data.local.entity.DeletedTaskEntity

@Dao
interface DeletedTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedTask(deletedTask: DeletedTaskEntity)

    @Query("SELECT * FROM deleted_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getDeletedTaskById(taskId: String): DeletedTaskEntity?

    @Query("SELECT * FROM deleted_tasks WHERE deletedAt > :since")
    suspend fun getDeletedTasksSince(since: Long): List<DeletedTaskEntity>

    @Query("DELETE FROM deleted_tasks WHERE deletedAt < :before")
    suspend fun cleanOldDeletedTasks(before: Long)

    @Query("DELETE FROM deleted_tasks WHERE id = :taskId")
    suspend fun deleteByTaskId(taskId: String)
}
