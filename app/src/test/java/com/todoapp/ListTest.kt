package com.todoapp

import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.TodoList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListTest {

    @Test
    fun `public list creation`() {
        val list = TodoList(
            id = "00000000-0000-0000-0000-000000000001",
            name = "Public",
            type = ListType.PUBLIC
        )
        assertEquals("Public", list.name)
        assertEquals(ListType.PUBLIC, list.type)
        assertNull(list.ownerId)
    }

    @Test
    fun `private list creation`() {
        val list = TodoList(
            id = "list-1",
            name = "Personal",
            type = ListType.PRIVATE,
            ownerId = "user-123"
        )
        assertEquals("Personal", list.name)
        assertEquals(ListType.PRIVATE, list.type)
        assertEquals("user-123", list.ownerId)
    }

    @Test
    fun `list type enum values`() {
        assertEquals(2, ListType.entries.size)
        assertTrue(ListType.entries.contains(ListType.PUBLIC))
        assertTrue(ListType.entries.contains(ListType.PRIVATE))
    }

    @Test
    fun `list copy with changes`() {
        val list = TodoList(id = "l1", name = "Work", type = ListType.PRIVATE, ownerId = "u1")
        val renamed = list.copy(name = "Office")
        assertEquals("Office", renamed.name)
        assertEquals("l1", renamed.id)
        assertEquals(ListType.PRIVATE, renamed.type)
    }

    @Test
    fun `public and private lists are different types`() {
        val public = TodoList(id = "p", name = "Public", type = ListType.PUBLIC)
        val private = TodoList(id = "r", name = "Personal", type = ListType.PRIVATE)
        assertNotEquals(public.type, private.type)
    }

    @Test
    fun `list timestamps`() {
        val now = System.currentTimeMillis()
        val list = TodoList(id = "l1", name = "Test", type = ListType.PRIVATE, createdAt = now, updatedAt = now)
        assertEquals(now, list.createdAt)
        assertEquals(now, list.updatedAt)
    }
}
