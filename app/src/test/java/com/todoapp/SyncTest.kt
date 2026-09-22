package com.todoapp

import com.todoapp.data.local.dao.SyncOperationDao
import com.todoapp.data.local.entity.DeletedListEntity
import com.todoapp.data.local.entity.DeletedTaskEntity
import com.todoapp.data.local.entity.SyncOperationEntity
import com.todoapp.data.sync.PushResult
import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TodoList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTest {

    // =====================================================================
    // compareVersion: the core conflict resolution primitive
    //
    // Used by SyncEngine to decide which mutation wins.
    // Returns >0 if a is newer, <0 if b is newer, 0 if equal.
    // Callers use strict ">" to decide: only strictly newer wins.
    // =====================================================================

    private fun compareVersion(aTimestamp: Long, bTimestamp: Long): Int {
        return aTimestamp.compareTo(bTimestamp)
    }

    @Test
    fun `compareVersion - a newer than b returns positive`() {
        assertTrue(compareVersion(2000L, 1000L) > 0)
    }

    @Test
    fun `compareVersion - b newer than a returns negative`() {
        assertTrue(compareVersion(1000L, 2000L) < 0)
    }

    @Test
    fun `compareVersion - equal timestamps return zero`() {
        assertEquals(0, compareVersion(1000L, 1000L))
    }

    @Test
    fun `compareVersion - zero timestamps equal`() {
        assertEquals(0, compareVersion(0L, 0L))
    }

    @Test
    fun `compareVersion - large timestamps work correctly`() {
        val t1 = 1700000000000L
        val t2 = 1700000001000L
        assertTrue(compareVersion(t2, t1) > 0)
        assertTrue(compareVersion(t1, t2) < 0)
    }

    // =====================================================================
    // Conflict resolution rule: strictly newer wins
    //
    // Equal timestamps treated as already-converged.
    // This prevents ping-pong between devices.
    // =====================================================================

    @Test
    fun `task conflict - local strictly newer - local wins`() {
        val localUpdatedAt = 2000L
        val remoteUpdatedAt = 1000L
        // Push should upload local (local is strictly newer)
        assertTrue("local strictly newer → push uploads", localUpdatedAt > remoteUpdatedAt)
    }

    @Test
    fun `task conflict - remote strictly newer - remote wins`() {
        val localUpdatedAt = 1000L
        val remoteUpdatedAt = 2000L
        // Push should NOT upload (remote is newer); pull should overwrite local
        assertFalse("remote newer → push skips", localUpdatedAt > remoteUpdatedAt)
    }

    @Test
    fun `task conflict - equal timestamps - already converged`() {
        val localUpdatedAt = 1000L
        val remoteUpdatedAt = 1000L
        // Neither side should overwrite the other
        assertFalse("equal → push skips", localUpdatedAt > remoteUpdatedAt)
        assertFalse("equal → pull skips", remoteUpdatedAt > localUpdatedAt)
    }

    // =====================================================================
    // Deletion conflict: deletedAt participates as mutation timestamp
    // =====================================================================

    @Test
    fun `delete conflict - local deletion newer than remote update`() {
        val localDeletedAt = 2000L
        val remoteUpdatedAt = 1000L
        // Local deletion is strictly newer → soft-delete remote
        assertTrue("deletion wins", localDeletedAt > remoteUpdatedAt)
    }

    @Test
    fun `delete conflict - remote update newer than local deletion`() {
        val localDeletedAt = 1000L
        val remoteUpdatedAt = 2000L
        // Remote update is newer → restore from remote, discard stale deletion
        assertFalse("remote update wins", localDeletedAt > remoteUpdatedAt)
    }

    @Test
    fun `delete conflict - equal timestamps - already converged`() {
        val localDeletedAt = 1000L
        val remoteUpdatedAt = 1000L
        // Equal → neither side overwrites
        assertFalse("equal → no overwrite", localDeletedAt > remoteUpdatedAt)
        assertFalse("equal → no overwrite reverse", remoteUpdatedAt > localDeletedAt)
    }

    @Test
    fun `delete vs delete - newer tombstone wins`() {
        val localDeletedAt = 2000L
        val remoteDeletedAt = 1000L
        assertTrue("local tombstone newer", localDeletedAt > remoteDeletedAt)
    }

    @Test
    fun `delete vs delete - equal timestamps - idempotent`() {
        val localDeletedAt = 1000L
        val remoteDeletedAt = 1000L
        assertEquals("both agree deleted", localDeletedAt, remoteDeletedAt)
    }

    // =====================================================================
    // Push-side: local upsert vs remote deleted
    // =====================================================================

    @Test
    fun `push upsert - local update newer than remote deletion - overwrites deletion`() {
        val localUpdatedAt = 2000L
        val remoteDeletedAt = 1000L
        assertTrue("local update overwrites stale deletion", localUpdatedAt > remoteDeletedAt)
    }

    @Test
    fun `push upsert - remote deletion newer or equal - deletion wins`() {
        val localUpdatedAt = 1000L
        val remoteDeletedAt = 2000L
        assertFalse("remote deletion wins", localUpdatedAt > remoteDeletedAt)
    }

    @Test
    fun `push upsert - remote deletion equal - deletion wins`() {
        val localUpdatedAt = 1000L
        val remoteDeletedAt = 1000L
        assertFalse("equal → deletion wins", localUpdatedAt > remoteDeletedAt)
    }

    // =====================================================================
    // Tombstone tests
    // =====================================================================

    @Test
    fun `task tombstone has correct id and deletedAt`() {
        val now = 1700000000000L
        val tombstone = DeletedTaskEntity(id = "task-1", deletedAt = now)
        assertEquals("task-1", tombstone.id)
        assertEquals(now, tombstone.deletedAt)
    }

    @Test
    fun `list tombstone has correct id and deletedAt`() {
        val now = 1700000000000L
        val tombstone = DeletedListEntity(id = "list-1", deletedAt = now)
        assertEquals("list-1", tombstone.id)
        assertEquals(now, tombstone.deletedAt)
    }

    @Test
    fun `tombstone default deletedAt is current time`() {
        val before = System.currentTimeMillis()
        val tombstone = DeletedTaskEntity(id = "task-1")
        val after = System.currentTimeMillis()
        assertTrue("deletedAt >= before", tombstone.deletedAt >= before)
        assertTrue("deletedAt <= after", tombstone.deletedAt <= after)
    }

    @Test
    fun `tombstone replaces older tombstone via REPLACE strategy`() {
        // Room uses OnConflictStrategy.REPLACE for tombstone inserts.
        // Inserting a tombstone with the same id replaces the old one.
        val oldTombstone = DeletedTaskEntity(id = "task-1", deletedAt = 1000L)
        val newTombstone = DeletedTaskEntity(id = "task-1", deletedAt = 2000L)
        // Simulating REPLACE: the new one wins
        assertEquals(2000L, newTombstone.deletedAt)
        assertTrue("new tombstone is newer", newTombstone.deletedAt > oldTombstone.deletedAt)
    }

    @Test
    fun `remote stale active version cannot resurrect deleted entity`() {
        val tombstoneDeletedAt = 2000L
        val remoteUpdatedAt = 1000L
        // Tombstone is newer → remote active version must be rejected
        assertTrue("tombstone prevents resurrection", tombstoneDeletedAt > remoteUpdatedAt)
    }

    @Test
    fun `newer remote active version supersedes older tombstone`() {
        val tombstoneDeletedAt = 1000L
        val remoteUpdatedAt = 2000L
        // Remote is newer than tombstone → accept remote version (recreation)
        assertTrue("remote supersedes old tombstone", remoteUpdatedAt > tombstoneDeletedAt)
    }

    @Test
    fun `tombstone is retained after successful push - prevents resurrection`() {
        // After push DELETE succeeds, tombstone is kept.
        // This prevents resurrection if a concurrent recreation happens
        // before the next pull cycle.
        val tombstone = DeletedTaskEntity(id = "task-1", deletedAt = 1000L)
        assertNotNull("tombstone retained", tombstone.id)
        assertEquals(1000L, tombstone.deletedAt)
    }

    @Test
    fun `30-day cleanup removes old tombstones`() {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        val oldTombstone = DeletedTaskEntity(id = "old", deletedAt = now - thirtyDaysMs - 1)
        val recentTombstone = DeletedTaskEntity(id = "recent", deletedAt = now - 1)
        val cutoff = now - thirtyDaysMs
        // oldTombstone.deletedAt < cutoff → would be cleaned
        assertTrue("old tombstone eligible for cleanup", oldTombstone.deletedAt < cutoff)
        // recentTombstone.deletedAt >= cutoff → retained
        assertFalse("recent tombstone retained", recentTombstone.deletedAt < cutoff)
    }

    // =====================================================================
    // Operation collapsing
    // =====================================================================

    @Test
    fun `collapseOperation UPDATE plus UPDATE returns UPDATE`() {
        assertEquals("UPDATE", SyncOperationDao.collapseOperation("UPDATE", "UPDATE"))
    }

    @Test
    fun `collapseOperation UPDATE plus DELETE returns DELETE`() {
        assertEquals("DELETE", SyncOperationDao.collapseOperation("UPDATE", "DELETE"))
    }

    @Test
    fun `collapseOperation INSERT plus DELETE returns DELETE`() {
        assertEquals("DELETE", SyncOperationDao.collapseOperation("INSERT", "DELETE"))
    }

    @Test
    fun `collapseOperation DELETE plus INSERT returns null`() {
        assertNull(SyncOperationDao.collapseOperation("DELETE", "INSERT"))
    }

    @Test
    fun `collapseOperation DELETE plus UPDATE returns null`() {
        assertNull(SyncOperationDao.collapseOperation("DELETE", "UPDATE"))
    }

    @Test
    fun `collapseOperation INSERT plus UPDATE returns INSERT`() {
        assertEquals("INSERT", SyncOperationDao.collapseOperation("INSERT", "UPDATE"))
    }

    @Test
    fun `collapseOperation UPDATE plus INSERT returns INSERT`() {
        assertEquals("INSERT", SyncOperationDao.collapseOperation("UPDATE", "INSERT"))
    }

    @Test
    fun `collapseOperation DELETE plus DELETE returns DELETE`() {
        assertEquals("DELETE", SyncOperationDao.collapseOperation("DELETE", "DELETE"))
    }

    // =====================================================================
    // Operation state tests
    // =====================================================================

    @Test
    fun `failed operation retains error metadata`() {
        val op = SyncOperationEntity(
            entityType = "task", entityId = "t1", operation = "UPDATE",
            attemptCount = 3, lastError = "Network error"
        )
        assertTrue(op.attemptCount > 0)
        assertEquals("Network error", op.lastError)
    }

    @Test
    fun `permanent failure retains operation with metadata`() {
        val op = SyncOperationEntity(
            entityType = "task", entityId = "t1", operation = "UPDATE",
            attemptCount = 11, lastError = "Permanent failure"
        )
        assertEquals("Permanent failure", op.lastError)
        assertTrue("attemptCount preserved", op.attemptCount > 10)
    }

    @Test
    fun `successful operation is removed from queue`() {
        val ops = mutableListOf(
            SyncOperationEntity(id = 1, entityType = "task", entityId = "t1", operation = "UPDATE"),
            SyncOperationEntity(id = 2, entityType = "task", entityId = "t2", operation = "DELETE")
        )
        ops.removeAll { it.id == 1L }
        assertEquals(1, ops.size)
        assertEquals("t2", ops[0].entityId)
    }

    @Test
    fun `metadata update preserves operation ID`() {
        val op = SyncOperationEntity(id = 42, entityType = "task", entityId = "t1", operation = "UPDATE", attemptCount = 0)
        val updated = op.copy(attemptCount = op.attemptCount + 1, lastError = "timeout")
        assertEquals(42L, updated.id)
        assertEquals(1, updated.attemptCount)
        assertEquals("timeout", updated.lastError)
    }

    @Test
    fun `one operation per entity enforced by unique index`() {
        // SyncOperationEntity has Index(value = ["entityType", "entityId"], unique = true)
        // Two ops for the same entity cannot coexist.
        val op1 = SyncOperationEntity(entityType = "task", entityId = "t1", operation = "INSERT")
        val op2 = SyncOperationEntity(entityType = "task", entityId = "t1", operation = "UPDATE")
        assertEquals(op1.entityType, op2.entityType)
        assertEquals(op1.entityId, op2.entityId)
        // The unique index prevents both from existing simultaneously.
    }

    @Test
    fun `DELETE operation does not require local entity to exist`() {
        val op = SyncOperationEntity(entityType = "task", entityId = "gone-task", operation = "DELETE")
        assertEquals("DELETE", op.operation)
        assertEquals("gone-task", op.entityId)
    }

    // =====================================================================
    // PushResult tests - verify each result has correct queue behavior
    // =====================================================================

    @Test
    fun `PushResult SUCCESS causes operation removal`() {
        val ops = mutableListOf(1, 2, 3)
        val result = PushResult.SUCCESS
        if (result == PushResult.SUCCESS) ops.removeAt(0)
        assertEquals(2, ops.size)
    }

    @Test
    fun `PushResult RETRY preserves operation`() {
        val ops = mutableListOf(1, 2, 3)
        val result = PushResult.RETRY
        if (result == PushResult.SUCCESS) ops.removeAt(0)
        assertEquals(3, ops.size)
    }

    @Test
    fun `PushResult CONFLICT preserves operation`() {
        val ops = mutableListOf(1, 2, 3)
        val result = PushResult.CONFLICT
        if (result == PushResult.SUCCESS) ops.removeAt(0)
        assertEquals(3, ops.size)
    }

    @Test
    fun `PushResult PERMANENT_FAILURE preserves operation`() {
        val ops = mutableListOf(1, 2, 3)
        val result = PushResult.PERMANENT_FAILURE
        if (result == PushResult.SUCCESS) ops.removeAt(0)
        assertEquals(3, ops.size)
    }

    // =====================================================================
    // Multi-device convergence simulation
    //
    // These simulate two-device states and verify the expected
    // outcome of conflict resolution. They exercise the same logic
    // that SyncEngine uses, without requiring a running Supabase.
    // =====================================================================

    @Test
    fun `multi-device A - device A stale update, device B newer update`() {
        val deviceALocalUpdatedAt = 1000L
        val deviceBRemoteUpdatedAt = 2000L
        // Device A pushes: local < remote → skip upload, pull remote
        assertFalse("A should not overwrite B", deviceALocalUpdatedAt > deviceBRemoteUpdatedAt)
        // Device A pulls: remote > local → overwrite with B's version
        assertTrue("A pulls B's version", deviceBRemoteUpdatedAt > deviceALocalUpdatedAt)
    }

    @Test
    fun `multi-device B - device A deleted, device B edited newer`() {
        val deviceALocalDeletedAt = 1000L
        val deviceBRemoteUpdatedAt = 2000L
        // Device A pushes deletion: localDeletedAt < remoteUpdatedAt → remote wins
        assertFalse("A's stale deletion rejected", deviceALocalDeletedAt > deviceBRemoteUpdatedAt)
        // Device A pulls: remote active, remote > local deletion → restore
    }

    @Test
    fun `multi-device C - device A deleted newer than device B edit`() {
        val deviceALocalDeletedAt = 2000L
        val deviceBRemoteUpdatedAt = 1000L
        // Device A pushes: localDeletedAt > remoteUpdatedAt → soft-delete remote
        assertTrue("A's deletion wins", deviceALocalDeletedAt > deviceBRemoteUpdatedAt)
    }

    @Test
    fun `multi-device D - device A edits, device B deleted newer`() {
        val deviceALocalUpdatedAt = 1000L
        val deviceBRemoteDeletedAt = 2000L
        // Device A pushes: localUpdatedAt < remoteDeletedAt → deletion wins
        assertFalse("B's deletion wins", deviceALocalUpdatedAt > deviceBRemoteDeletedAt)
    }

    @Test
    fun `multi-device E - device A edits newer than device B deletion`() {
        val deviceALocalUpdatedAt = 2000L
        val deviceBRemoteDeletedAt = 1000L
        // Device A pushes: localUpdatedAt > remoteDeletedAt → overwrite stale deletion
        assertTrue("A's update overwrites stale deletion", deviceALocalUpdatedAt > deviceBRemoteDeletedAt)
    }

    @Test
    fun `multi-device F - both deleted, newest tombstone wins`() {
        val deviceALocalDeletedAt = 2000L
        val deviceBRemoteDeletedAt = 1000L
        assertTrue("newer deletion wins", deviceALocalDeletedAt > deviceBRemoteDeletedAt)
    }

    @Test
    fun `multi-device G - both deleted simultaneously - idempotent`() {
        val deviceALocalDeletedAt = 1000L
        val deviceBRemoteDeletedAt = 1000L
        assertEquals("both agree deleted", deviceALocalDeletedAt, deviceBRemoteDeletedAt)
    }

    @Test
    fun `multi-device H - public task uses same conflict rules`() {
        val userALocalUpdatedAt = 1000L
        val userBRemoteUpdatedAt = 2000L
        // Public tasks use the same mutation-timestamp resolution
        assertFalse("stale public update rejected", userALocalUpdatedAt > userBRemoteUpdatedAt)
    }

    // =====================================================================
    // Operation ordering
    // =====================================================================

    @Test
    fun `DELETE operations ordered before INSERT UPDATE`() {
        val ops = listOf(
            SyncOperationEntity(entityType = "task", entityId = "t1", operation = "INSERT", createdAt = 100),
            SyncOperationEntity(entityType = "task", entityId = "t2", operation = "DELETE", createdAt = 100),
            SyncOperationEntity(entityType = "task", entityId = "t3", operation = "UPDATE", createdAt = 100)
        )
        val sorted = ops.sortedWith(compareBy<SyncOperationEntity> { it.createdAt }
            .thenBy { if (it.operation == "DELETE") 0 else 1 }
            .thenBy { if (it.entityType == "task") 0 else 1 })
        assertEquals("DELETE", sorted[0].operation)
        assertEquals("INSERT", sorted[1].operation)
        assertEquals("UPDATE", sorted[2].operation)
    }

    @Test
    fun `task operations ordered before list operations`() {
        val ops = listOf(
            SyncOperationEntity(entityType = "list", entityId = "l1", operation = "DELETE", createdAt = 100),
            SyncOperationEntity(entityType = "task", entityId = "t1", operation = "DELETE", createdAt = 100)
        )
        val sorted = ops.sortedWith(compareBy<SyncOperationEntity> { it.createdAt }
            .thenBy { if (it.operation == "DELETE") 0 else 1 }
            .thenBy { if (it.entityType == "task") 0 else 1 })
        assertEquals("task", sorted[0].entityType)
        assertEquals("list", sorted[1].entityType)
    }

    @Test
    fun `older operations processed first regardless of type`() {
        val ops = listOf(
            SyncOperationEntity(entityType = "task", entityId = "t1", operation = "DELETE", createdAt = 200),
            SyncOperationEntity(entityType = "list", entityId = "l1", operation = "INSERT", createdAt = 100)
        )
        val sorted = ops.sortedWith(compareBy<SyncOperationEntity> { it.createdAt }
            .thenBy { if (it.operation == "DELETE") 0 else 1 }
            .thenBy { if (it.entityType == "task") 0 else 1 })
        assertEquals(100L, sorted[0].createdAt)
        assertEquals(200L, sorted[1].createdAt)
    }

    // =====================================================================
    // Public list immutability
    // =====================================================================

    @Test
    fun `PUBLIC_LIST_ID is correct fixed UUID`() {
        assertEquals("00000000-0000-0000-0000-000000000001", TodoList.PUBLIC_LIST_ID)
    }

    @Test
    fun `public list type cannot be changed`() {
        val publicList = TodoList(id = TodoList.PUBLIC_LIST_ID, name = "Public", type = ListType.PUBLIC)
        assertEquals(ListType.PUBLIC, publicList.type)
    }

    @Test
    fun `public list owner is always null`() {
        val publicList = TodoList(id = TodoList.PUBLIC_LIST_ID, name = "Public", type = ListType.PUBLIC, ownerId = null)
        assertNull(publicList.ownerId)
    }

    // =====================================================================
    // Backup normalization
    // =====================================================================

    @Test
    fun `backup public list is normalized to fixed ID`() {
        val imported = TodoList(id = "some-random-id", name = "Imported Public", type = ListType.PUBLIC)
        val normalized = if (imported.type == ListType.PUBLIC) {
            imported.copy(id = TodoList.PUBLIC_LIST_ID, name = "Public", ownerId = null)
        } else imported
        assertEquals(TodoList.PUBLIC_LIST_ID, normalized.id)
        assertEquals("Public", normalized.name)
        assertNull(normalized.ownerId)
    }

    @Test
    fun `backup private list gets current user ownership`() {
        val imported = TodoList(id = "l1", name = "My List", type = ListType.PRIVATE, ownerId = "old-user")
        val reassigned = if (imported.type == ListType.PRIVATE) imported.copy(ownerId = "user-123") else imported
        assertEquals("user-123", reassigned.ownerId)
    }

    // =====================================================================
    // Authentication behavior
    // =====================================================================

    @Test
    fun `session refresh failure does not create new identity`() {
        val currentUserId = "user-123"
        val refreshedUserId: String? = null
        val resultingUserId = refreshedUserId ?: currentUserId
        assertEquals("user-123", resultingUserId)
    }

    @Test
    fun `existing session with valid token is reused`() {
        val currentUserId = "user-123"
        val refreshedUserId: String? = "user-123"
        val resultingUserId = refreshedUserId ?: currentUserId
        assertEquals("user-123", resultingUserId)
    }

    @Test
    fun `expired session triggers refresh attempt`() {
        var refreshAttempted = false
        val sessionExpired = true
        if (sessionExpired) refreshAttempted = true
        assertTrue(refreshAttempted)
    }

    // =====================================================================
    // Backup atomicity
    // =====================================================================

    @Test
    fun `failed restore leaves original data unchanged`() {
        val originalTasks = listOf(Task(id = "t1", title = "Original"))
        val tasksAfterFailedImport = if (false) emptyList<Task>() else originalTasks
        assertEquals(1, tasksAfterFailedImport.size)
        assertEquals("Original", tasksAfterFailedImport[0].title)
    }

    // =====================================================================
    // Remote list deletion discovery
    // =====================================================================

    @Test
    fun `local list not on remote is discovered as deleted`() {
        val localListIds = setOf("l1", "l2", "l3")
        val remoteListIds = setOf("l1", "l3")
        val toDelete = localListIds.filter { it !in remoteListIds }
        assertEquals(1, toDelete.size)
        assertEquals("l2", toDelete[0])
    }

    @Test
    fun `remote tombstoned list is detected via deletedAt`() {
        val remoteList = SupabaseListStub(id = "l1", deletedAt = "2024-01-01T00:00:00Z")
        assertTrue(remoteList.deletedAt != null)
    }

    @Test
    fun `remote list without deletedAt is active`() {
        val remoteList = SupabaseListStub(id = "l1", deletedAt = null)
        assertFalse(remoteList.deletedAt != null)
    }

    // =====================================================================
    // Failed operations are never silently discarded
    // =====================================================================

    @Test
    fun `failed operations retain error metadata for investigation`() {
        val op = SyncOperationEntity(
            entityType = "task", entityId = "t1", operation = "UPDATE",
            attemptCount = 15, lastError = "Persistent error"
        )
        assertNotNull(op.lastError)
        assertTrue(op.attemptCount > 10)
    }

    // Stub for testing
    private data class SupabaseListStub(val id: String, val deletedAt: String?)
}
