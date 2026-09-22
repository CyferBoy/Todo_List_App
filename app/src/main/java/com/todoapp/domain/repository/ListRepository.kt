package com.todoapp.domain.repository

import com.todoapp.domain.model.TodoList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun getAllLists(): Flow<List<TodoList>>
    suspend fun getAllListsList(): List<TodoList>
    suspend fun getPublicList(): TodoList?
    fun getPublicListFlow(): Flow<TodoList?>
    fun getPrivateListsByOwner(ownerId: String): Flow<List<TodoList>>
    suspend fun getListById(listId: String): TodoList?
    fun getListByIdFlow(listId: String): Flow<TodoList?>
    suspend fun insertList(list: TodoList)
    suspend fun updateList(list: TodoList)
    suspend fun deleteList(list: TodoList)
    suspend fun deleteListById(listId: String)
    suspend fun updateListOwner(listId: String, newOwnerId: String)
    suspend fun deleteListWithTaskTombstones(listId: String)
}
