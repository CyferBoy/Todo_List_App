package com.todoapp.domain.usecase.list

import com.todoapp.domain.model.TodoList
import com.todoapp.domain.repository.ListRepository

class DeleteListUseCase(private val repository: ListRepository) {
    suspend operator fun invoke(listId: String) {
        if (listId == TodoList.PUBLIC_LIST_ID) return
        repository.deleteListWithTaskTombstones(listId)
    }
}
