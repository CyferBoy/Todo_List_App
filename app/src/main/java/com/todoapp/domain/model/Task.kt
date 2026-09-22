package com.todoapp.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class Priority { NONE, LOW, MEDIUM, HIGH }

enum class RecurrenceType { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

data class Task(
    val id: String,
    val listId: String? = null,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.NONE,
    val categoryId: String? = null,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val position: Int = 0,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: List<Int> = emptyList(),
    val recurrenceEndDate: LocalDate? = null,
    val reminderDate: LocalDate? = null,
    val reminderTime: LocalTime? = null,
    val isReminderEnabled: Boolean = false,
    val parentTaskId: String? = null,
    val originalDueDate: LocalDate? = null,
    val categoryName: String? = null,
    val categoryColor: Long? = null
)
