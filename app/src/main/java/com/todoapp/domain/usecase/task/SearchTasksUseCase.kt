package com.todoapp.domain.usecase.task

import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class SearchTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(query: String): Flow<List<Task>> = repository.searchTasks(query)
}
