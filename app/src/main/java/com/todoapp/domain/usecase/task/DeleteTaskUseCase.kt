package com.todoapp.domain.usecase.task

import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.TaskRepository

class DeleteTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) {
        repository.deleteTaskWithTombstone(task.id)
    }

    suspend operator fun invoke(taskId: String) {
        repository.deleteTaskWithTombstone(taskId)
    }
}
