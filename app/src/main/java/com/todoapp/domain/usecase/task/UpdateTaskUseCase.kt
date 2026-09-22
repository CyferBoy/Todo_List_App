package com.todoapp.domain.usecase.task

import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.TaskRepository

class UpdateTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) {
        repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }
}
