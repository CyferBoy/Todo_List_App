package com.todoapp.domain.usecase.category

import com.todoapp.domain.repository.CategoryRepository

class DeleteCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(categoryId: String) {
        repository.deleteCategoryById(categoryId)
    }
}
