package com.todoapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.RecurrenceType

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("dueDate"), Index("isCompleted"), Index("position"), Index("listId")]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String? = null,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.NONE,
    val categoryId: String? = null,
    val dueDate: Long? = null,
    val dueTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val position: Int = 0,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: String = "",
    val recurrenceEndDate: Long? = null,
    val reminderDate: Long? = null,
    val reminderTime: Long? = null,
    val isReminderEnabled: Boolean = false,
    val parentTaskId: String? = null,
    val originalDueDate: Long? = null
)
