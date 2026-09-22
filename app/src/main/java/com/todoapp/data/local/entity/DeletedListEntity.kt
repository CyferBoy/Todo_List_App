package com.todoapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_lists")
data class DeletedListEntity(
    @PrimaryKey val id: String,
    val deletedAt: Long = System.currentTimeMillis()
)
