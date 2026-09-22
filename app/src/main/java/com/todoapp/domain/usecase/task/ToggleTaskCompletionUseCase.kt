package com.todoapp.domain.usecase.task

import android.content.Context
import com.todoapp.data.sync.SyncEngine
import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.model.Task
import com.todoapp.domain.repository.TaskRepository
import com.todoapp.domain.util.IdGenerator
import com.todoapp.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class ToggleTaskCompletionUseCase(
    private val repository: TaskRepository,
    private val context: Context,
    private val syncEngine: SyncEngine
) {
    suspend operator fun invoke(taskId: String, completed: Boolean) {
        repository.updateTaskCompletion(taskId, completed)
        val task = repository.getTaskById(taskId) ?: return

        if (completed) {
            NotificationHelper.cancelReminder(context, task.id)
            if (task.recurrenceType != RecurrenceType.NONE) {
                val nextTask = createNextOccurrence(task)
                if (nextTask != null) {
                    syncEngine.enqueueTaskOperation(nextTask.id, "INSERT")
                    if (nextTask.isReminderEnabled &&
                        nextTask.reminderDate != null && nextTask.reminderTime != null
                    ) {
                        val reminderDateTime = LocalDateTime.of(nextTask.reminderDate, nextTask.reminderTime)
                        if (reminderDateTime.isAfter(LocalDateTime.now())) {
                            NotificationHelper.scheduleReminder(context, nextTask)
                        }
                    }
                }
            }
        } else {
            if (task.isReminderEnabled && task.reminderDate != null && task.reminderTime != null) {
                val reminderDateTime = LocalDateTime.of(task.reminderDate, task.reminderTime)
                if (reminderDateTime.isAfter(LocalDateTime.now())) {
                    NotificationHelper.scheduleReminder(context, task)
                }
            }
        }
    }

    private suspend fun createNextOccurrence(task: Task): Task? {
        val currentDueDate = task.dueDate ?: return null
        val nextDate = calculateNextDate(currentDueDate, task) ?: return null

        if (isDuplicateOccurrence(task.id, nextDate)) {
            return null
        }

        val nextTask = task.copy(
            id = IdGenerator.generate(),
            isCompleted = false,
            dueDate = nextDate,
            reminderDate = nextDate,
            parentTaskId = task.id,
            originalDueDate = task.originalDueDate ?: task.dueDate,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            position = repository.getNextPosition()
        )
        repository.insertTask(nextTask)
        return nextTask
    }

    private fun calculateNextDate(current: java.time.LocalDate, task: Task): java.time.LocalDate? {
        val type = task.recurrenceType
        val interval = task.recurrenceInterval
        val endDate = task.recurrenceEndDate

        val nextDate = when (type) {
            RecurrenceType.DAILY -> current.plusDays(interval.toLong())
            RecurrenceType.WEEKLY -> calculateNextWeeklyDate(current, task)
            RecurrenceType.MONTHLY -> calculateNextMonthlyDate(current, interval)
            RecurrenceType.YEARLY -> calculateNextYearlyDate(current, interval)
            RecurrenceType.NONE -> null
        } ?: return null

        if (endDate != null && nextDate.isAfter(endDate)) {
            return null
        }

        return nextDate
    }

    private fun calculateNextWeeklyDate(current: java.time.LocalDate, task: Task): java.time.LocalDate? {
        val daysOfWeek = task.recurrenceDaysOfWeek
            .map { it }
            .filter { it in 0..6 }

        if (daysOfWeek.isEmpty()) {
            return current.plusWeeks(task.recurrenceInterval.toLong())
        }

        for (daysAhead in 1..7) {
            val candidate = current.plusDays(daysAhead.toLong())
            if (candidate.dayOfWeek.value % 7 in daysOfWeek) {
                return candidate
            }
        }
        return current.plusWeeks(task.recurrenceInterval.toLong())
    }

    private fun calculateNextMonthlyDate(current: java.time.LocalDate, interval: Int): java.time.LocalDate {
        var nextMonth = current.monthValue + interval
        var nextYear = current.year
        while (nextMonth > 12) {
            nextMonth -= 12
            nextYear++
        }
        val maxDay = java.time.YearMonth.of(nextYear, nextMonth).lengthOfMonth()
        val nextDay = minOf(current.dayOfMonth, maxDay)
        return java.time.LocalDate.of(nextYear, nextMonth, nextDay)
    }

    private fun calculateNextYearlyDate(current: java.time.LocalDate, interval: Int): java.time.LocalDate {
        val nextYear = current.year + interval
        if (current.monthValue == 2 && current.dayOfMonth == 29) {
            if (!java.time.Year.isLeap(nextYear.toLong())) {
                return java.time.LocalDate.of(nextYear, 2, 28)
            }
        }
        return current.plusYears(interval.toLong())
    }

    private suspend fun isDuplicateOccurrence(parentTaskId: String, dueDate: java.time.LocalDate): Boolean {
        val allTasks = repository.getAllTasks().first()
        return allTasks.any {
            it.parentTaskId == parentTaskId && it.dueDate == dueDate
        }
    }
}
