package com.todoapp.domain.model

enum class ListType { PUBLIC, PRIVATE }

data class TodoList(
    val id: String,
    val name: String,
    val type: ListType,
    val ownerId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val PUBLIC_LIST_ID = "00000000-0000-0000-0000-000000000001"
    }
}
