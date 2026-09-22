package com.todoapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.todoapp.data.local.entity.SyncOperationEntity

@Dao
abstract class SyncOperationDao {
    @Query("""
        SELECT * FROM sync_operations
        ORDER BY createdAt ASC,
            CASE operation WHEN 'DELETE' THEN 0 ELSE 1 END ASC,
            CASE entityType WHEN 'task' THEN 0 ELSE 1 END ASC
    """)
    abstract suspend fun getAllPendingList(): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE entityType = :type AND entityId = :entityId LIMIT 1")
    abstract suspend fun getByEntity(type: String, entityId: String): SyncOperationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(operation: SyncOperationEntity)

    @Query("UPDATE sync_operations SET attemptCount = :attemptCount, lastError = :lastError WHERE id = :id")
    abstract suspend fun updateMetadata(id: Long, attemptCount: Int, lastError: String?)

    @Query("DELETE FROM sync_operations WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_operations WHERE entityType = :type AND entityId = :entityId")
    abstract suspend fun deleteByEntity(type: String, entityId: String)

    @Query("DELETE FROM sync_operations")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun enqueueTaskOperation(taskId: String, operation: String) {
        val existing = getByEntity("task", taskId)
        if (existing != null) {
            val collapsed = collapseOperation(existing.operation, operation)
            if (collapsed == null) {
                deleteByEntity("task", taskId)
                return
            }
            insert(existing.copy(operation = collapsed, createdAt = System.currentTimeMillis(), attemptCount = 0, lastError = null))
        } else {
            insert(SyncOperationEntity(entityType = "task", entityId = taskId, operation = operation))
        }
    }

    @Transaction
    open suspend fun enqueueListOperation(listId: String, operation: String) {
        val existing = getByEntity("list", listId)
        if (existing != null) {
            val collapsed = collapseOperation(existing.operation, operation)
            if (collapsed == null) {
                deleteByEntity("list", listId)
                return
            }
            insert(existing.copy(operation = collapsed, createdAt = System.currentTimeMillis(), attemptCount = 0, lastError = null))
        } else {
            insert(SyncOperationEntity(entityType = "list", entityId = listId, operation = operation))
        }
    }

    companion object {
        fun collapseOperation(existing: String, incoming: String): String? {
            if (existing == "DELETE" && incoming == "DELETE") return "DELETE"
            if (existing == "DELETE" && incoming != "DELETE") return null
            if (incoming == "DELETE") return "DELETE"
            if (existing == "INSERT" && incoming == "UPDATE") return "INSERT"
            if (existing == "UPDATE" && incoming == "UPDATE") return "UPDATE"
            return incoming
        }
    }
}
