package com.todoapp.domain.usecase.task

import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class GetTasksByDateRangeUseCase(private val repository: TaskRepository) {
    operator fun invoke(start: LocalDate, end: LocalDate): Flow<List<Task>> =
        repository.getTasksBetweenDates(start, end)
}
