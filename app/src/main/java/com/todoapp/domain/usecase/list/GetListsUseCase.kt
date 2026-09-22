package com.todoapp.domain.usecase.list

import com.todoapp.domain.model.TodoList
import com.todoapp.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow

class GetListsUseCase(private val repository: ListRepository) {
    operator fun invoke(): Flow<List<TodoList>> = repository.getAllLists()
    suspend fun getPublicList() = repository.getPublicList()
    fun getPublicListFlow(): Flow<TodoList?> = repository.getPublicListFlow()
    fun getPrivateLists(ownerId: String): Flow<List<TodoList>> = repository.getPrivateListsByOwner(ownerId)
}
