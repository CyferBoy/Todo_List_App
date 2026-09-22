package com.todoapp.domain.model

data class Category(
    val id: String,
    val name: String,
    val color: Long = 0xFF6750A4,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
