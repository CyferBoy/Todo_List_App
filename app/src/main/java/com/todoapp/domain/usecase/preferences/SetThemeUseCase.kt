package com.todoapp.domain.usecase.preferences

import com.todoapp.domain.model.ThemeMode
import com.todoapp.domain.repository.PreferencesRepository

class SetThemeUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
