package com.todoapp

import com.todoapp.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryTest {

    @Test
    fun `category creation with defaults`() {
        val category = Category(id = "cat1", name = "Work")
        assertEquals("cat1", category.id)
        assertEquals("Work", category.name)
        assertEquals(0xFF6750A4, category.color)
        assertEquals(0, category.position)
    }

    @Test
    fun `category with custom values`() {
        val category = Category(
            id = "cat2",
            name = "Personal",
            color = 0xFF006D3C,
            position = 2,
            createdAt = 1000L
        )
        assertEquals("cat2", category.id)
        assertEquals("Personal", category.name)
        assertEquals(0xFF006D3C, category.color)
        assertEquals(2, category.position)
        assertEquals(1000L, category.createdAt)
    }

    @Test
    fun `category equality by id`() {
        val cat1 = Category(id = "cat1", name = "Work")
        val cat2 = Category(id = "cat1", name = "Work Updated")
        assertEquals(cat1.id, cat2.id)
    }

    @Test
    fun `category copy with changes`() {
        val original = Category(id = "cat1", name = "Work")
        val renamed = original.copy(name = "Office")
        assertEquals("Office", renamed.name)
        assertEquals("cat1", renamed.id)
    }
}
