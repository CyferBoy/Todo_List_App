package com.todoapp.domain.usecase.backup

import android.net.Uri
import com.todoapp.domain.repository.BackupRepository

class ExportTasksUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(uri: Uri): Boolean = repository.exportToFile(uri)
}
