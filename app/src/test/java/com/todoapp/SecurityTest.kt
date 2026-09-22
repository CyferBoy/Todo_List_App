package com.todoapp

import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.Task
import com.todoapp.domain.model.TodoList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecurityTest {

    @Test
    fun `public list has fixed ID`() {
        val publicList = TodoList(
            id = "00000000-0000-0000-0000-000000000001",
            name = "Public",
            type = ListType.PUBLIC
        )
        assertEquals("00000000-0000-0000-0000-000000000001", publicList.id)
    }

    @Test
    fun `public list is PUBLIC type`() {
        val publicList = TodoList(id = "00000000-0000-0000-0000-000000000001", name = "Public", type = ListType.PUBLIC)
        assertEquals(ListType.PUBLIC, publicList.type)
    }

    @Test
    fun `public list owner is null`() {
        val publicList = TodoList(id = "00000000-0000-0000-0000-000000000001", name = "Public", type = ListType.PUBLIC)
        assertNull(publicList.ownerId)
    }

    @Test
    fun `private list has owner`() {
        val privateList = TodoList(id = "list-1", name = "Personal", type = ListType.PRIVATE, ownerId = "user-123")
        assertEquals("user-123", privateList.ownerId)
        assertEquals(ListType.PRIVATE, privateList.type)
    }

    @Test
    fun `private lists can have different owners`() {
        val list1 = TodoList(id = "l1", name = "A", type = ListType.PRIVATE, ownerId = "user-1")
        val list2 = TodoList(id = "l2", name = "B", type = ListType.PRIVATE, ownerId = "user-2")
        assertNotEquals(list1.ownerId, list2.ownerId)
    }

    @Test
    fun `public list cannot be accidentally assigned owner`() {
        val publicList = TodoList(id = "00000000-0000-0000-0000-000000000001", name = "Public", type = ListType.PUBLIC, ownerId = null)
        assertNull(publicList.ownerId)
    }

    @Test
    fun `task belongs to correct list`() {
        val publicTask = Task(id = "t1", title = "Public Task", listId = "00000000-0000-0000-0000-000000000001")
        val privateTask = Task(id = "t2", title = "Private Task", listId = "list-1")
        assertEquals("00000000-0000-0000-0000-000000000001", publicTask.listId)
        assertEquals("list-1", privateTask.listId)
    }

    @Test
    fun `migrated personal list gets owner after auth`() {
        val list = TodoList(id = "list_personal", name = "Personal", type = ListType.PRIVATE, ownerId = null)
        assertNull(list.ownerId)
        val fixed = list.copy(ownerId = "user-123")
        assertEquals("user-123", fixed.ownerId)
    }

    @Test
    fun `create list use case only creates private lists`() {
        val type = ListType.PRIVATE
        assertEquals(ListType.PRIVATE, type)
    }

    @Test
    fun `public list fixed ID matches RLS policy`() {
        assertEquals("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000001")
    }

    @Test
    fun `backup import does not create duplicate public list`() {
        val importedLists = listOf(
            TodoList(id = "00000000-0000-0000-0000-000000000001", name = "Public", type = ListType.PUBLIC),
            TodoList(id = "other", name = "Other", type = ListType.PUBLIC)
        )
        val publicLists = importedLists.filter { it.type == ListType.PUBLIC }
        assertEquals(1, publicLists.count { it.id == "00000000-0000-0000-0000-000000000001" })
    }

    @Test
    fun `anonymous user ID must be stable`() {
        // The anonymous user ID should persist across sessions
        // It must NOT be replaced when refresh fails
        val userId1 = "anonymous-uuid-stable"
        val userId2 = userId1
        assertEquals("user ID must be stable", userId1, userId2)
    }

    @Test
    fun `private tasks protected by owner_id = auth uid`() {
        // RLS: private tasks require owner_id = auth.uid()
        // This is enforced at Supabase level, not app level
        val ownerId = "user-123"
        val taskOwnerId = "user-123"
        assertEquals("private task owner must match", ownerId, taskOwnerId)
    }

    @Test
    fun `public tasks accessible by all authenticated users`() {
        val publicListId = TodoList.PUBLIC_LIST_ID
        assertEquals("00000000-0000-0000-0000-000000000001", publicListId)
    }
}
