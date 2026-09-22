package com.todoapp.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTask(
    val id: String,
    @SerialName("list_id")
    val listId: String,
    val title: String,
    val description: String = "",
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    val priority: String = "NONE",
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("due_time")
    val dueTime: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    val position: Int = 0,
    @SerialName("recurrence_type")
    val recurrenceType: String = "NONE",
    @SerialName("recurrence_interval")
    val recurrenceInterval: Int = 1,
    @SerialName("recurrence_days_of_week")
    val recurrenceDaysOfWeek: String = "",
    @SerialName("recurrence_end_date")
    val recurrenceEndDate: String? = null,
    @SerialName("reminder_enabled")
    val reminderEnabled: Boolean = false,
    @SerialName("reminder_date")
    val reminderDate: String? = null,
    @SerialName("reminder_time")
    val reminderTime: String? = null,
    @SerialName("parent_task_id")
    val parentTaskId: String? = null,
    @SerialName("original_due_date")
    val originalDueDate: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)
