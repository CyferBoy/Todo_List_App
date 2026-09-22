package com.todoapp.domain.usecase.category

import com.todoapp.domain.model.Category
import com.todoapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke(): Flow<List<Category>> = repository.getAllCategories()
    suspend fun list(): List<Category> = repository.getAllCategoriesList()
}
