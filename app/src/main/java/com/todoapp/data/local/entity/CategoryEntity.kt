package com.todoapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long = 0xFF6750A4,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
