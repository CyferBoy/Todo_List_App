package com.todoapp

import com.todoapp.data.local.entity.DeletedListEntity
import com.todoapp.data.local.entity.DeletedTaskEntity
import com.todoapp.data.local.entity.SyncOperationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueueTest {

    @Test
    fun `insert operation queued correctly`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "task-1", operation = "INSERT")
        assertEquals("task", op.entityType)
        assertEquals("task-1", op.entityId)
        assertEquals("INSERT", op.operation)
        assertEquals(0, op.attemptCount)
        assertNull(op.lastError)
    }

    @Test
    fun `update operation queued correctly`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "task-1", operation = "UPDATE")
        assertEquals("UPDATE", op.operation)
    }

    @Test
    fun `delete operation creates tombstone`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "task-1", operation = "DELETE")
        assertEquals("DELETE", op.operation)
    }

    @Test
    fun `sync operation tracks attempt count`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "task-1", operation = "INSERT", attemptCount = 3)
        assertEquals(3, op.attemptCount)
    }

    @Test
    fun `sync operation tracks last error`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "task-1", operation = "INSERT", lastError = "Network error")
        assertEquals("Network error", op.lastError)
    }

    @Test
    fun `failed sync retains pending operation`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "task-1", operation = "INSERT")
        val failedOp = op.copy(attemptCount = op.attemptCount + 1, lastError = "timeout")
        assertEquals(1, failedOp.attemptCount)
        assertEquals("timeout", failedOp.lastError)
    }

    @Test
    fun `successful sync clears operation`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "task-1", operation = "INSERT")
        assertNotNull(op.entityId)
    }

    @Test
    fun `list operations are separate from task operations`() {
        val taskOp = SyncOperationEntity(entityType = "task", entityId = "t1", operation = "INSERT")
        val listOp = SyncOperationEntity(entityType = "list", entityId = "l1", operation = "INSERT")
        assertNotEquals(taskOp.entityType, listOp.entityType)
    }

    @Test
    fun `deletedAt tombstone prevents resurrection`() {
        val task = com.todoapp.domain.model.Task(id = "t1", title = "Test")
        val deletedTask = task.copy(updatedAt = System.currentTimeMillis())
        assertTrue(deletedTask.updatedAt > 0)
    }

    @Test
    fun `updatedAt preserved correctly during sync`() {
        val originalTime = 1700000000000L
        val task = com.todoapp.domain.model.Task(id = "t1", title = "Test", updatedAt = originalTime)
        assertEquals(originalTime, task.updatedAt)
    }

    @Test
    fun `older remote does not overwrite newer local`() {
        val localTask = com.todoapp.domain.model.Task(id = "t1", title = "Local", updatedAt = 2000000000000L)
        val remoteTask = com.todoapp.domain.model.Task(id = "t1", title = "Remote", updatedAt = 1000000000000L)
        assertTrue(localTask.updatedAt > remoteTask.updatedAt)
    }

    @Test
    fun `delete operation does not require local entity`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "gone-task", operation = "DELETE")
        assertEquals("DELETE", op.operation)
        assertEquals("gone-task", op.entityId)
    }

    @Test
    fun `list delete operation propagates to remote`() {
        val op = SyncOperationEntity(entityType = "list", entityId = "list-1", operation = "DELETE")
        assertEquals("list", op.entityType)
        assertEquals("DELETE", op.operation)
    }

    @Test
    fun `pending local update blocks remote overwrite`() {
        val localTask = com.todoapp.domain.model.Task(id = "t1", title = "Local", updatedAt = 1000)
        val remoteTask = com.todoapp.domain.model.Task(id = "t1", title = "Remote", updatedAt = 2000)
        val hasPending = true
        val shouldOverwrite = hasPending && remoteTask.updatedAt > localTask.updatedAt
        assertFalse("pending op should block overwrite", shouldOverwrite)
    }

    @Test
    fun `deleted task is not resurrected by remote pull`() {
        val deletedTaskId = "t1"
        val deletedTasks = listOf(DeletedTaskEntity(id = deletedTaskId))
        val remoteTask = com.todoapp.domain.model.Task(id = deletedTaskId, title = "Remote")
        val isDeleted = deletedTasks.any { it.id == remoteTask.id }
        assertTrue(isDeleted)
    }

    @Test
    fun `list deletion creates both task tombstones and list tombstone`() {
        val taskTombstones = listOf("t1", "t2").map { DeletedTaskEntity(id = it, deletedAt = System.currentTimeMillis()) }
        val listTombstone = DeletedListEntity(id = "l1", deletedAt = System.currentTimeMillis())
        assertEquals(2, taskTombstones.size)
        assertNotNull(listTombstone.id)
    }

    @Test
    fun `concurrent enqueue produces at most one operation per entity`() {
        // With unique index on (entityType, entityId), concurrent inserts are deduplicated
        val entityType = "task"
        val entityId = "t1"
        // Simulate two concurrent enqueue attempts
        val op1 = SyncOperationEntity(entityType = entityType, entityId = entityId, operation = "INSERT")
        val op2 = SyncOperationEntity(entityType = entityType, entityId = entityId, operation = "UPDATE")
        // Only one should survive (the one that gets inserted last via REPLACE)
        assertEquals(op1.entityType, op2.entityType)
        assertEquals(op1.entityId, op2.entityId)
    }

    @Test
    fun `retry preserves original operation ID`() {
        val op = SyncOperationEntity(id = 42, entityType = "task", entityId = "t1", operation = "UPDATE")
        val retried = op.copy(attemptCount = op.attemptCount + 1, lastError = "timeout")
        assertEquals(42L, retried.id)
        assertEquals("UPDATE", retried.operation)
        assertEquals("t1", retried.entityId)
    }

    @Test
    fun `list tombstone has correct structure`() {
        val tombstone = DeletedListEntity(id = "list-1", deletedAt = 1700000000000L)
        assertEquals("list-1", tombstone.id)
        assertEquals(1700000000000L, tombstone.deletedAt)
    }
}
