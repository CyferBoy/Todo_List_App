package com.todoapp.domain.repository

import com.todoapp.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getAllCategoriesList(): List<Category>
    suspend fun getCategoryById(categoryId: String): Category?
    suspend fun getCategoryByName(name: String): Category?
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun deleteCategoryById(categoryId: String)
    suspend fun getNextPosition(): Int
}
