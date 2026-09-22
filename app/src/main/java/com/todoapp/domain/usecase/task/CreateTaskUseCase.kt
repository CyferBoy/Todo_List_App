package com.todoapp.domain.usecase.task

import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.TaskRepository
import com.todoapp.domain.util.IdGenerator

class CreateTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task): Task {
        val newTask = task.copy(
            id = if (task.id.isEmpty()) IdGenerator.generate() else task.id,
            position = repository.getNextPosition(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.insertTask(newTask)
        return newTask
    }
}
