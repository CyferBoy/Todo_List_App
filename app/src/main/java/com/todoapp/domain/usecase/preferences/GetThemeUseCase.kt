package com.todoapp.domain.usecase.preferences

import com.todoapp.domain.model.ThemeMode
import com.todoapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class GetThemeUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
