package com.todoapp.domain.usecase.category

import com.todoapp.domain.model.Category
import com.todoapp.domain.repository.CategoryRepository
import com.todoapp.domain.util.IdGenerator

class CreateCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(name: String, color: Long = 0xFF6750A4): Category {
        val category = Category(
            id = IdGenerator.generate(),
            name = name.trim(),
            color = color,
            position = repository.getNextPosition()
        )
        repository.insertCategory(category)
        return category
    }
}
