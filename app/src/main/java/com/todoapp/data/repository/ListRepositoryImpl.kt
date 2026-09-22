package com.todoapp.data.repository

import androidx.room.withTransaction
import com.todoapp.data.local.dao.DeletedListDao
import com.todoapp.data.local.dao.TodoListDao
import com.todoapp.data.local.entity.DeletedListEntity
import com.todoapp.data.local.entity.TodoListEntity
import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ListRepositoryImpl(
    private val todoListDao: TodoListDao,
    private val database: com.todoapp.data.local.database.TodoDatabase,
    private val taskDao: com.todoapp.data.local.dao.TaskDao,
    private val deletedTaskDao: com.todoapp.data.local.dao.DeletedTaskDao,
    private val syncOperationDao: com.todoapp.data.local.dao.SyncOperationDao,
    private val deletedListDao: DeletedListDao
) : ListRepository {

    override fun getAllLists(): Flow<List<TodoList>> = todoListDao.getAllLists().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getAllListsList(): List<TodoList> =
        todoListDao.getAllListsList().map { it.toDomain() }

    override suspend fun getPublicList(): TodoList? =
        todoListDao.getPublicList()?.toDomain()

    override fun getPublicListFlow(): Flow<TodoList?> =
        todoListDao.getPublicListFlow().map { it?.toDomain() }

    override fun getPrivateListsByOwner(ownerId: String): Flow<List<TodoList>> =
        todoListDao.getPrivateListsByOwner(ownerId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getListById(listId: String): TodoList? =
        todoListDao.getListById(listId)?.toDomain()

    override fun getListByIdFlow(listId: String): Flow<TodoList?> =
        todoListDao.getListByIdFlow(listId).map { it?.toDomain() }

    override suspend fun insertList(list: TodoList) = todoListDao.insertList(list.toEntity())

    override suspend fun updateList(list: TodoList) = todoListDao.updateList(list.toEntity())

    override suspend fun deleteList(list: TodoList) = todoListDao.deleteList(list.toEntity())

    override suspend fun deleteListById(listId: String) = todoListDao.deleteListById(listId)

    override suspend fun deleteListWithTaskTombstones(listId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val taskIds = taskDao.getTaskIdsByListId(listId)
            for (taskId in taskIds) {
                deletedTaskDao.insertDeletedTask(
                    com.todoapp.data.local.entity.DeletedTaskEntity(id = taskId, deletedAt = now)
                )
                syncOperationDao.enqueueTaskOperation(taskId, "DELETE")
            }
            deletedListDao.insertDeletedList(DeletedListEntity(id = listId, deletedAt = now))
            syncOperationDao.enqueueListOperation(listId, "DELETE")
            todoListDao.deleteListById(listId)
        }
    }

    override suspend fun updateListOwner(listId: String, newOwnerId: String) {
        val list = todoListDao.getListById(listId) ?: return
        todoListDao.updateList(list.copy(ownerId = newOwnerId, updatedAt = System.currentTimeMillis()))
    }

    private fun TodoListEntity.toDomain(): TodoList = TodoList(
        id = id,
        name = name,
        type = when (type) {
            "public" -> ListType.PUBLIC
            else -> ListType.PRIVATE
        },
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TodoList.toEntity(): TodoListEntity = TodoListEntity(
        id = id,
        name = name,
        type = type.name.lowercase(),
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
