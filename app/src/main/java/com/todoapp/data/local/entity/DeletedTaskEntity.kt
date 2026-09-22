package com.todoapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_tasks")
data class DeletedTaskEntity(
    @PrimaryKey val id: String,
    val deletedAt: Long = System.currentTimeMillis()
)
