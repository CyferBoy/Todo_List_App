package com.todoapp.domain.usecase.list

import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.TodoList
import com.todoapp.domain.repository.ListRepository
import com.todoapp.domain.util.IdGenerator

class CreateListUseCase(private val repository: ListRepository) {
    suspend operator fun invoke(name: String, ownerId: String? = null): TodoList {
        val list = TodoList(
            id = IdGenerator.generate(),
            name = name.trim(),
            type = ListType.PRIVATE,
            ownerId = ownerId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.insertList(list)
        return list
    }
}
