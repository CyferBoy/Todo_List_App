package com.todoapp.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.todoapp.data.local.dao.DeletedListDao
import com.todoapp.data.local.dao.DeletedTaskDao
import com.todoapp.data.local.dao.SyncOperationDao
import com.todoapp.data.local.entity.DeletedListEntity
import com.todoapp.data.local.entity.DeletedTaskEntity
import com.todoapp.data.local.entity.SyncOperationEntity
import com.todoapp.data.remote.SupabaseClientProvider
import com.todoapp.data.remote.SupabaseConfig
import com.todoapp.data.remote.SupabaseAuthService
import com.todoapp.data.remote.model.SupabaseList
import com.todoapp.data.remote.model.SupabaseTask
import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.ListRepository
import com.todoapp.domain.repository.TaskRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG = "SyncEngine"

enum class SyncStatus {
    SYNCED, OFFLINE, SYNCING, SYNC_FAILED
}

enum class PushResult {
    SUCCESS, RETRY, CONFLICT, PERMANENT_FAILURE
}

/**
 * Offline-first sync engine with timestamp-based conflict resolution.
 *
 * ## Conflict rule
 *
 * The newer mutation wins. A mutation is either:
 * - an active update  → represented by `updatedAt`
 * - a deletion        → represented by `deletedAt`
 *
 * "Newer" means strictly greater timestamp (`>`).
 * Equal timestamps are treated as already-converged: push does not upload,
 * pull does not overwrite. This avoids ping-pong between devices.
 *
 * ## SQL trigger interaction
 *
 * The Supabase schema has a `BEFORE UPDATE` trigger that sets
 * `updated_at = now()` on every row modification. This means after any
 * upsert, `updated_at` reflects the server time of the write, not the
 * app's original mutation time. This is safe because:
 *
 * 1. Conflict comparison always reads the remote `updated_at` BEFORE
 *    performing the upsert, using the pre-write value.
 * 2. The trigger makes the winning version's `updated_at` strictly newer
 *    than both contenders, so the losing side always sees the remote as
 *    newer on its next pull and converges.
 *
 * ## Race window (SELECT + UPSERT is not atomic)
 *
 * Each push follows: SELECT remote → compare timestamps → UPSERT.
 * If another device modifies the record between SELECT and UPSERT, the
 * local version may overwrite a newer remote version. The Supabase
 * Kotlin SDK does not support conditional upserts (e.g. "only write if
 * updated_at still equals the value I read"). This is an inherent
 * limitation of client-side conflict resolution without server-side
 * logic. The next sync cycle will correct any drift.
 *
 * ## Tombstone lifecycle
 *
 * - Created when a local entity is deleted (via `deleteTaskWithTombstone`
 *   or `deleteListWithTaskTombstones`).
 * - Retained after successful push to prevent resurrection if a
 *   concurrent recreation occurs before the next pull.
 * - Cleaned up after 30 days by `cleanOldDeletedTasks`/`cleanOldDeletedLists`.
 * - During pull, tombstones are updated (never reduced) to the max of
 *   local and remote deletion timestamps.
 */
class SyncEngine(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
    private val syncOperationDao: SyncOperationDao,
    private val deletedTaskDao: DeletedTaskDao,
    private val deletedListDao: DeletedListDao
) {
    private var _status: SyncStatus = SyncStatus.OFFLINE
    val status: SyncStatus get() = _status

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun syncAll(): SyncStatus {
        if (!SupabaseConfig.isConfigured || !isOnline()) {
            _status = SyncStatus.OFFLINE
            return SyncStatus.OFFLINE
        }

        _status = SyncStatus.SYNCING
        return withContext(Dispatchers.IO) {
            try {
                val userId = SupabaseAuthService.ensureSession(context)
                if (userId == null) {
                    Log.w(TAG, "No authenticated session — skipping sync")
                    _status = SyncStatus.SYNC_FAILED
                    return@withContext SyncStatus.SYNC_FAILED
                }
                deletedTaskDao.cleanOldDeletedTasks(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                deletedListDao.cleanOldDeletedLists(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                val pushFailed = pushLocalChanges()
                pullRemoteChanges()
                _status = if (pushFailed) SyncStatus.SYNC_FAILED else SyncStatus.SYNCED
                _status
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _status = SyncStatus.SYNC_FAILED
                SyncStatus.SYNC_FAILED
            }
        }
    }

    suspend fun enqueueTaskOperation(taskId: String, operation: String) {
        syncOperationDao.enqueueTaskOperation(taskId, operation)
    }

    suspend fun enqueueListOperation(listId: String, operation: String) {
        syncOperationDao.enqueueListOperation(listId, operation)
    }

    suspend fun enqueueTaskDeletion(taskId: String) {
        deletedTaskDao.insertDeletedTask(DeletedTaskEntity(id = taskId))
        syncOperationDao.enqueueTaskOperation(taskId, "DELETE")
    }

    suspend fun enqueueListDeletion(listId: String) {
        deletedListDao.insertDeletedList(DeletedListEntity(id = listId))
        syncOperationDao.enqueueListOperation(listId, "DELETE")
    }

    suspend fun cancelPendingDeletion(taskId: String) {
        deletedTaskDao.deleteByTaskId(taskId)
        syncOperationDao.deleteByEntity("task", taskId)
    }

    private suspend fun hasPendingTaskOperation(taskId: String): Boolean {
        return syncOperationDao.getByEntity("task", taskId) != null
    }

    private suspend fun hasPendingListOperation(listId: String): Boolean {
        return syncOperationDao.getByEntity("list", listId) != null
    }

    // =====================================================================
    // Push: upload local changes, resolving conflicts against remote state
    // =====================================================================

    private suspend fun pushLocalChanges(): Boolean {
        var anyFailed = false
        val client = SupabaseClientProvider.getClient()
        val pendingOps = syncOperationDao.getAllPendingList()

        for (op in pendingOps) {
            val result = when (op.entityType) {
                "task" -> pushTaskOperation(client, op)
                "list" -> pushListOperation(client, op)
                else -> PushResult.PERMANENT_FAILURE
            }
            when (result) {
                PushResult.SUCCESS -> {
                    syncOperationDao.deleteById(op.id)
                    // Tombstones are retained after successful push.
                    // They are cleaned up after 30 days by cleanOldDeletedTasks/cleanOldDeletedLists.
                    // This prevents resurrection if a concurrent recreation occurs
                    // before the next pull cycle.
                }
                PushResult.RETRY -> {
                    syncOperationDao.updateMetadata(
                        id = op.id,
                        attemptCount = op.attemptCount + 1,
                        lastError = "Retry requested"
                    )
                    anyFailed = true
                }
                PushResult.CONFLICT -> {
                    Log.w(TAG, "Conflict for ${op.entityType}:${op.entityId}, retrying later")
                    syncOperationDao.updateMetadata(
                        id = op.id,
                        attemptCount = op.attemptCount + 1,
                        lastError = "Conflict detected"
                    )
                    anyFailed = true
                }
                PushResult.PERMANENT_FAILURE -> {
                    Log.e(TAG, "Permanent failure for ${op.entityType}:${op.entityId} — retaining op for investigation")
                    syncOperationDao.updateMetadata(
                        id = op.id,
                        attemptCount = op.attemptCount + 1,
                        lastError = "Permanent failure"
                    )
                    anyFailed = true
                }
            }
        }
        return anyFailed
    }

    /**
     * Compare two mutation timestamps.
     * Returns > 0 if a is newer, < 0 if b is newer, 0 if equal.
     *
     * Callers use `>` (strictly newer) to decide conflict winners.
     * Equal timestamps are treated as already-converged.
     */
    private fun compareVersion(aTimestamp: Long, bTimestamp: Long): Int {
        return aTimestamp.compareTo(bTimestamp)
    }

    private suspend fun pushTaskOperation(client: io.github.jan.supabase.SupabaseClient, op: SyncOperationEntity): PushResult {
        return when (op.operation) {
            "DELETE" -> pushTaskDelete(client, op)
            "INSERT", "UPDATE" -> pushTaskUpsert(client, op)
            else -> PushResult.PERMANENT_FAILURE
        }
    }

    /**
     * Push a local task deletion to the remote.
     *
     * Scenarios:
     * - Remote already deleted (with equal or newer deletedAt) → already converged, done.
     * - Remote deleted but older → overwrite with newer local deletion.
     * - Remote active, local deletion newer → soft-delete remote.
     * - Remote active, remote update newer → remote wins, restore locally, discard stale deletion.
     * - Remote doesn't exist → already gone, done.
     */
    private suspend fun pushTaskDelete(client: io.github.jan.supabase.SupabaseClient, op: SyncOperationEntity): PushResult {
        val localTombstone = deletedTaskDao.getDeletedTaskById(op.entityId)
        val localDeletedAt = localTombstone?.deletedAt ?: System.currentTimeMillis()

        val remote = client.from("tasks")
            .select { filter { eq("id", op.entityId) } }
            .decodeList<SupabaseTask>()
            .firstOrNull()

        if (remote == null) {
            // Remote doesn't exist. Deletion already applied. Done.
            return PushResult.SUCCESS
        }

        if (remote.deletedAt != null) {
            // Both sides deleted. Keep the newest tombstone.
            val remoteDeletedAt = isoToEpoch(remote.deletedAt)
            if (remoteDeletedAt > localDeletedAt) {
                // Remote deletion is newer — update local tombstone.
                deletedTaskDao.insertDeletedTask(DeletedTaskEntity(id = op.entityId, deletedAt = remoteDeletedAt))
            }
            // Either way, both agree the entity is deleted. Converged.
            return PushResult.SUCCESS
        }

        // Remote is active. Compare deletion timestamp against remote's last update.
        val remoteUpdatedAt = isoToEpoch(remote.updatedAt)
        val cmp = compareVersion(localDeletedAt, remoteUpdatedAt)
        if (cmp > 0) {
            // Local deletion is strictly newer → soft-delete the remote.
            client.from("tasks").upsert(remote.copy(deletedAt = epochToIso(localDeletedAt)))
            return PushResult.SUCCESS
        } else {
            // Remote update is newer or equal → remote wins. Restore locally.
            taskRepository.insertTask(remote.toDomainTask())
            deletedTaskDao.deleteByTaskId(op.entityId)
            return PushResult.SUCCESS
        }
    }

    /**
     * Push a local task upsert (INSERT or UPDATE) to the remote.
     *
     * Scenarios:
     * - Local entity missing, tombstone exists → tombstone may need to overwrite remote active.
     * - Local entity missing, no tombstone → cannot proceed, retry later.
     * - Remote doesn't exist → upload local.
     * - Remote deleted, local update newer → overwrite stale deletion.
     * - Remote deleted, remote deletion newer or equal → deletion wins, accept.
     * - Remote active, local newer → upload local.
     * - Remote active, remote newer → pull remote locally.
     * - Remote active, equal → already converged, skip.
     */
    private suspend fun pushTaskUpsert(client: io.github.jan.supabase.SupabaseClient, op: SyncOperationEntity): PushResult {
        val local = taskRepository.getTaskById(op.entityId)

        if (local == null) {
            // Local entity doesn't exist. Check if a tombstone can overwrite remote.
            val tombstone = deletedTaskDao.getDeletedTaskById(op.entityId)
            if (tombstone != null) {
                val remote = client.from("tasks")
                    .select { filter { eq("id", op.entityId) } }
                    .decodeList<SupabaseTask>()
                    .firstOrNull()
                if (remote == null) return PushResult.SUCCESS
                if (remote.deletedAt != null) return PushResult.SUCCESS
                val remoteUpdatedAt = isoToEpoch(remote.updatedAt)
                if (tombstone.deletedAt > remoteUpdatedAt) {
                    // Tombstone is newer than remote active → soft-delete remote.
                    client.from("tasks").upsert(remote.copy(deletedAt = epochToIso(tombstone.deletedAt)))
                }
                return PushResult.SUCCESS
            }
            Log.w(TAG, "Task ${op.entityId} missing locally with no tombstone — retaining operation")
            return PushResult.RETRY
        }

        // Local entity exists. Fetch remote.
        val remote = client.from("tasks")
            .select { filter { eq("id", op.entityId) } }
            .decodeList<SupabaseTask>()
            .firstOrNull()

        if (remote == null) {
            // Remote doesn't exist → upload local.
            client.from("tasks").upsert(local.toSupabaseTask())
            return PushResult.SUCCESS
        }

        if (remote.deletedAt != null) {
            // Remote is deleted. Does local update supersede it?
            val remoteDeletedAt = isoToEpoch(remote.deletedAt)
            val cmp = compareVersion(local.updatedAt, remoteDeletedAt)
            if (cmp > 0) {
                // Local update is strictly newer → overwrite stale deletion.
                client.from("tasks").upsert(local.toSupabaseTask())
                return PushResult.SUCCESS
            } else {
                // Remote deletion is newer or equal → deletion wins. Accept.
                return PushResult.SUCCESS
            }
        }

        // Both active. Compare timestamps.
        val remoteUpdatedAt = isoToEpoch(remote.updatedAt)
        val cmp = compareVersion(local.updatedAt, remoteUpdatedAt)
        if (cmp > 0) {
            // Local is strictly newer → upload local.
            client.from("tasks").upsert(local.toSupabaseTask())
            return PushResult.SUCCESS
        } else {
            // Remote is newer or equal → pull remote locally.
            taskRepository.insertTask(remote.toDomainTask())
            return PushResult.SUCCESS
        }
    }

    private suspend fun pushListOperation(client: io.github.jan.supabase.SupabaseClient, op: SyncOperationEntity): PushResult {
        return when (op.operation) {
            "DELETE" -> pushListDelete(client, op)
            "INSERT", "UPDATE" -> pushListUpsert(client, op)
            else -> PushResult.PERMANENT_FAILURE
        }
    }

    /**
     * Push a local list deletion to the remote.
     * Same logic as pushTaskDelete, applied to lists.
     */
    private suspend fun pushListDelete(client: io.github.jan.supabase.SupabaseClient, op: SyncOperationEntity): PushResult {
        val localTombstone = deletedListDao.getDeletedListById(op.entityId)
        val localDeletedAt = localTombstone?.deletedAt ?: System.currentTimeMillis()

        val remote = client.from("lists")
            .select { filter { eq("id", op.entityId) } }
            .decodeList<SupabaseList>()
            .firstOrNull()

        if (remote == null) {
            return PushResult.SUCCESS
        }

        if (remote.deletedAt != null) {
            // Both deleted. Keep newest tombstone.
            val remoteDeletedAt = isoToEpoch(remote.deletedAt)
            if (remoteDeletedAt > localDeletedAt) {
                deletedListDao.insertDeletedList(DeletedListEntity(id = op.entityId, deletedAt = remoteDeletedAt))
            }
            return PushResult.SUCCESS
        }

        val remoteUpdatedAt = isoToEpoch(remote.updatedAt)
        val cmp = compareVersion(localDeletedAt, remoteUpdatedAt)
        if (cmp > 0) {
            // Local deletion newer → soft-delete remote.
            client.from("lists").upsert(remote.copy(deletedAt = epochToIso(localDeletedAt)))
            return PushResult.SUCCESS
        } else {
            // Remote update newer or equal → restore remote locally.
            listRepository.insertList(
                TodoList(
                    id = remote.id, name = remote.name, type = ListType.PRIVATE,
                    ownerId = remote.ownerId,
                    createdAt = isoToEpoch(remote.createdAt),
                    updatedAt = remoteUpdatedAt
                )
            )
            deletedListDao.deleteByListId(op.entityId)
            return PushResult.SUCCESS
        }
    }

    /**
     * Push a local list upsert to the remote.
     * Same logic as pushTaskUpsert, applied to lists.
     */
    private suspend fun pushListUpsert(client: io.github.jan.supabase.SupabaseClient, op: SyncOperationEntity): PushResult {
        val local = listRepository.getListById(op.entityId)

        if (local == null) {
            val tombstone = deletedListDao.getDeletedListById(op.entityId)
            if (tombstone != null) {
                val remote = client.from("lists")
                    .select { filter { eq("id", op.entityId) } }
                    .decodeList<SupabaseList>()
                    .firstOrNull()
                if (remote == null) return PushResult.SUCCESS
                if (remote.deletedAt != null) return PushResult.SUCCESS
                val remoteUpdatedAt = isoToEpoch(remote.updatedAt)
                if (tombstone.deletedAt > remoteUpdatedAt) {
                    client.from("lists").upsert(remote.copy(deletedAt = epochToIso(tombstone.deletedAt)))
                }
                return PushResult.SUCCESS
            }
            Log.w(TAG, "List ${op.entityId} missing locally with no tombstone — retaining operation")
            return PushResult.RETRY
        }

        val remote = client.from("lists")
            .select { filter { eq("id", op.entityId) } }
            .decodeList<SupabaseList>()
            .firstOrNull()

        if (remote == null) {
            val supabaseList = SupabaseList(
                id = local.id, name = local.name, type = local.type.name.lowercase(),
                ownerId = local.ownerId,
                createdAt = epochToIso(local.createdAt), updatedAt = epochToIso(local.updatedAt)
            )
            client.from("lists").upsert(supabaseList)
            return PushResult.SUCCESS
        }

        if (remote.deletedAt != null) {
            val remoteDeletedAt = isoToEpoch(remote.deletedAt)
            val cmp = compareVersion(local.updatedAt, remoteDeletedAt)
            if (cmp > 0) {
                val supabaseList = SupabaseList(
                    id = local.id, name = local.name, type = local.type.name.lowercase(),
                    ownerId = local.ownerId,
                    createdAt = epochToIso(local.createdAt), updatedAt = epochToIso(local.updatedAt)
                )
                client.from("lists").upsert(supabaseList)
                return PushResult.SUCCESS
            } else {
                return PushResult.SUCCESS
            }
        }

        val remoteUpdatedAt = isoToEpoch(remote.updatedAt)
        val cmp = compareVersion(local.updatedAt, remoteUpdatedAt)
        if (cmp > 0) {
            val supabaseList = SupabaseList(
                id = local.id, name = local.name, type = local.type.name.lowercase(),
                ownerId = local.ownerId,
                createdAt = epochToIso(local.createdAt), updatedAt = epochToIso(local.updatedAt)
            )
            client.from("lists").upsert(supabaseList)
            return PushResult.SUCCESS
        } else {
            listRepository.insertList(
                TodoList(
                    id = remote.id, name = remote.name, type = ListType.PRIVATE,
                    ownerId = remote.ownerId,
                    createdAt = isoToEpoch(remote.createdAt),
                    updatedAt = remoteUpdatedAt
                )
            )
            return PushResult.SUCCESS
        }
    }

    // =====================================================================
    // Pull: fetch remote changes and apply to local Room database
    // =====================================================================

    private suspend fun pullRemoteChanges() {
        val client = SupabaseClientProvider.getClient()

        // Load local tombstones for resurrection prevention.
        val deletedTasks = deletedTaskDao.getDeletedTasksSince(0)
        val deletedTaskIds = deletedTasks.map { it.id }.toSet()
        val deletedLists = deletedListDao.getDeletedListsSince(0)
        val deletedListIds = deletedLists.map { it.id }.toSet()

        // --- Pull public tasks ---
        val publicListId = TodoList.PUBLIC_LIST_ID
        val publicTasks = client.from("tasks")
            .select {
                filter { eq("list_id", publicListId) }
                order("updated_at", Order.DESCENDING)
            }
            .decodeList<SupabaseTask>()

        for (st in publicTasks) {
            if (hasPendingTaskOperation(st.id)) continue

            if (st.deletedAt != null) {
                val remoteDeletedAt = isoToEpoch(st.deletedAt)
                // Update tombstone to max of local and remote deletion timestamps.
                val existingTombstone = deletedTaskDao.getDeletedTaskById(st.id)
                val tombstoneDeletedAt = maxOf(remoteDeletedAt, existingTombstone?.deletedAt ?: 0L)
                deletedTaskDao.insertDeletedTask(DeletedTaskEntity(id = st.id, deletedAt = tombstoneDeletedAt))
                // Delete local active entity if remote deletion is strictly newer.
                val local = taskRepository.getTaskById(st.id)
                if (local != null) {
                    val cmp = compareVersion(remoteDeletedAt, local.updatedAt)
                    if (cmp > 0) {
                        taskRepository.deleteTaskById(st.id)
                    }
                }
                continue
            }
            if (st.id in deletedTaskIds) continue

            val remoteUpdatedAt = isoToEpoch(st.updatedAt)
            val task = st.toDomainTask()
            val existing = taskRepository.getTaskById(task.id)
            if (existing != null && hasPendingTaskOperation(task.id)) continue
            val localTombstone = deletedTaskDao.getDeletedTaskById(task.id)
            if (localTombstone != null && localTombstone.deletedAt > remoteUpdatedAt) continue
            if (existing == null || remoteUpdatedAt > existing.updatedAt) {
                taskRepository.insertTask(task)
            }
        }

        // --- Pull private lists and their tasks ---
        val userId = SupabaseAuthService.getCurrentUserId(context) ?: return
        val privateLists = client.from("lists")
            .select { filter { eq("owner_id", userId) } }
            .decodeList<SupabaseList>()

        for (sl in privateLists) {
            if (sl.deletedAt != null) {
                if (hasPendingListOperation(sl.id)) continue
                val localList = listRepository.getListById(sl.id)
                if (localList != null) {
                    val remoteDeletedAt = isoToEpoch(sl.deletedAt)
                    val cmp = compareVersion(remoteDeletedAt, localList.updatedAt)
                    if (cmp > 0) {
                        listRepository.deleteListWithTaskTombstones(sl.id)
                    }
                }
                continue
            }
            if (sl.id in deletedListIds) continue
            if (hasPendingListOperation(sl.id)) continue

            val localList = listRepository.getListById(sl.id)
            if (localList == null) {
                listRepository.insertList(
                    TodoList(
                        id = sl.id, name = sl.name, type = ListType.PRIVATE,
                        ownerId = sl.ownerId,
                        createdAt = isoToEpoch(sl.createdAt),
                        updatedAt = isoToEpoch(sl.updatedAt)
                    )
                )
            } else {
                val remoteUpdatedAt = isoToEpoch(sl.updatedAt)
                if (remoteUpdatedAt > localList.updatedAt) {
                    listRepository.insertList(
                        TodoList(
                            id = sl.id, name = sl.name, type = ListType.PRIVATE,
                            ownerId = sl.ownerId,
                            createdAt = isoToEpoch(sl.createdAt),
                            updatedAt = remoteUpdatedAt
                        )
                    )
                }
            }

            // Pull tasks belonging to this private list.
            val listTasks = client.from("tasks")
                .select { filter { eq("list_id", sl.id) } }
                .decodeList<SupabaseTask>()

            for (st in listTasks) {
                if (hasPendingTaskOperation(st.id)) continue

                if (st.deletedAt != null) {
                    val remoteDeletedAt = isoToEpoch(st.deletedAt)
                    val existingTombstone = deletedTaskDao.getDeletedTaskById(st.id)
                    val tombstoneDeletedAt = maxOf(remoteDeletedAt, existingTombstone?.deletedAt ?: 0L)
                    deletedTaskDao.insertDeletedTask(DeletedTaskEntity(id = st.id, deletedAt = tombstoneDeletedAt))
                    val local = taskRepository.getTaskById(st.id)
                    if (local != null) {
                        val cmp = compareVersion(remoteDeletedAt, local.updatedAt)
                        if (cmp > 0) {
                            taskRepository.deleteTaskById(st.id)
                        }
                    }
                    continue
                }
                if (st.id in deletedTaskIds) continue

                val remoteUpdatedAt = isoToEpoch(st.updatedAt)
                val task = st.toDomainTask()
                val existing = taskRepository.getTaskById(task.id)
                if (existing != null && hasPendingTaskOperation(task.id)) continue
                val localTombstone = deletedTaskDao.getDeletedTaskById(task.id)
                if (localTombstone != null && localTombstone.deletedAt > remoteUpdatedAt) continue
                if (existing == null || remoteUpdatedAt > existing.updatedAt) {
                    taskRepository.insertTask(task)
                }
            }
        }

        // Detect local lists that no longer exist on remote (deleted by another device).
        try {
            val allRemoteLists = client.from("lists")
                .select { filter { eq("owner_id", userId) } }
                .decodeList<SupabaseList>()
            val remoteListIds = allRemoteLists.map { it.id }.toSet()
            val localLists = listRepository.getAllListsList()
            for (localList in localLists) {
                if (localList.id == TodoList.PUBLIC_LIST_ID) continue
                if (localList.id in remoteListIds) continue
                if (hasPendingListOperation(localList.id)) continue
                listRepository.deleteListWithTaskTombstones(localList.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Remote list discovery query failed — skipping local deletion scan", e)
        }
    }

    // =====================================================================
    // Conversion helpers
    // =====================================================================

    private fun Task.toSupabaseTask(): SupabaseTask {
        return SupabaseTask(
            id = id,
            listId = listId ?: TodoList.PUBLIC_LIST_ID,
            title = title,
            description = description,
            isCompleted = isCompleted,
            priority = priority.name,
            categoryId = categoryId,
            dueDate = dueDate?.toString(),
            dueTime = dueTime?.toString(),
            createdAt = epochToIso(createdAt),
            updatedAt = epochToIso(updatedAt),
            isPinned = isPinned,
            position = position,
            recurrenceType = recurrenceType.name,
            recurrenceInterval = recurrenceInterval,
            recurrenceDaysOfWeek = recurrenceDaysOfWeek.joinToString(","),
            recurrenceEndDate = recurrenceEndDate?.toString(),
            reminderEnabled = isReminderEnabled,
            reminderDate = reminderDate?.toString(),
            reminderTime = reminderTime?.toString(),
            parentTaskId = parentTaskId,
            originalDueDate = originalDueDate?.toString()
        )
    }

    private fun SupabaseTask.toDomainTask(): Task {
        return Task(
            id = id,
            listId = listId,
            title = title,
            description = description,
            isCompleted = isCompleted,
            priority = try { com.todoapp.domain.model.Priority.valueOf(priority) } catch (_: Exception) { com.todoapp.domain.model.Priority.NONE },
            categoryId = categoryId,
            dueDate = dueDate?.let { try { java.time.LocalDate.parse(it) } catch (_: Exception) { null } },
            dueTime = dueTime?.let { try { java.time.LocalTime.parse(it) } catch (_: Exception) { null } },
            createdAt = isoToEpoch(createdAt),
            updatedAt = isoToEpoch(updatedAt),
            isPinned = isPinned,
            position = position,
            recurrenceType = try { com.todoapp.domain.model.RecurrenceType.valueOf(recurrenceType) } catch (_: Exception) { com.todoapp.domain.model.RecurrenceType.NONE },
            recurrenceInterval = recurrenceInterval,
            recurrenceDaysOfWeek = recurrenceDaysOfWeek.split(",").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 },
            recurrenceEndDate = recurrenceEndDate?.let { try { java.time.LocalDate.parse(it) } catch (_: Exception) { null } },
            isReminderEnabled = reminderEnabled,
            reminderDate = reminderDate?.let { try { java.time.LocalDate.parse(it) } catch (_: Exception) { null } },
            reminderTime = reminderTime?.let { try { java.time.LocalTime.parse(it) } catch (_: Exception) { null } },
            parentTaskId = parentTaskId,
            originalDueDate = originalDueDate?.let { try { java.time.LocalDate.parse(it) } catch (_: Exception) { null } }
        )
    }

    private fun epochToIso(millis: Long): String {
        return Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }

    private fun isoToEpoch(iso: String): Long {
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
