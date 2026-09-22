package com.todoapp.domain.repository

import android.net.Uri
import com.todoapp.domain.model.Category
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TodoList

interface BackupRepository {
    suspend fun exportToFile(uri: Uri): Boolean
    suspend fun importFromFile(uri: Uri): Result<Unit>
}

data class BackupData(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val tasks: List<Task>,
    val categories: List<Category>,
    val lists: List<TodoList> = emptyList()
)
