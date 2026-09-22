package com.todoapp.domain.usecase.backup

import android.net.Uri
import com.todoapp.domain.repository.BackupRepository

class ImportTasksUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(uri: Uri): Result<Unit> = repository.importFromFile(uri)
}
