package com.todoapp.data.repository

import com.todoapp.data.local.dao.CategoryDao
import com.todoapp.data.local.entity.CategoryEntity
import com.todoapp.domain.model.Category
import com.todoapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(private val categoryDao: CategoryDao) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getAllCategoriesList(): List<Category> =
        categoryDao.getAllCategoriesList().map { it.toDomain() }

    override suspend fun getCategoryById(categoryId: String): Category? =
        categoryDao.getCategoryById(categoryId)?.toDomain()

    override suspend fun getCategoryByName(name: String): Category? =
        categoryDao.getCategoryByName(name)?.toDomain()

    override suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category.toEntity())

    override suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category.toEntity())

    override suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category.toEntity())

    override suspend fun deleteCategoryById(categoryId: String) = categoryDao.deleteCategoryById(categoryId)

    override suspend fun getNextPosition(): Int = (categoryDao.getMaxPosition() ?: -1) + 1

    private fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        name = name,
        color = color,
        position = position,
        createdAt = createdAt
    )

    private fun Category.toEntity(): CategoryEntity = CategoryEntity(
        id = id,
        name = name,
        color = color,
        position = position,
        createdAt = createdAt
    )
}
