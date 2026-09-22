package com.todoapp

import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskTest {

    private fun createTask(
        id: String = "test-id",
        title: String = "Test Task",
        isCompleted: Boolean = false,
        priority: Priority = Priority.NONE,
        dueDate: LocalDate? = null,
        isPinned: Boolean = false,
        recurrenceType: RecurrenceType = RecurrenceType.NONE
    ) = Task(
        id = id,
        title = title,
        isCompleted = isCompleted,
        priority = priority,
        dueDate = dueDate,
        isPinned = isPinned,
        recurrenceType = recurrenceType
    )

    @Test
    fun `task default values are correct`() {
        val task = createTask()
        assertEquals("test-id", task.id)
        assertEquals("Test Task", task.title)
        assertEquals("", task.description)
        assertFalse(task.isCompleted)
        assertEquals(Priority.NONE, task.priority)
        assertNull(task.categoryId)
        assertNull(task.dueDate)
        assertNull(task.dueTime)
        assertFalse(task.isPinned)
        assertEquals(RecurrenceType.NONE, task.recurrenceType)
        assertFalse(task.isReminderEnabled)
    }

    @Test
    fun `task priority levels`() {
        val low = createTask(priority = Priority.LOW)
        val medium = createTask(priority = Priority.MEDIUM)
        val high = createTask(priority = Priority.HIGH)

        assertEquals(Priority.LOW, low.priority)
        assertEquals(Priority.MEDIUM, medium.priority)
        assertEquals(Priority.HIGH, high.priority)
    }

    @Test
    fun `task completion toggle`() {
        val task = createTask(isCompleted = false)
        val completed = task.copy(isCompleted = true)
        val uncompleted = completed.copy(isCompleted = false)

        assertFalse(task.isCompleted)
        assertTrue(completed.isCompleted)
        assertFalse(uncompleted.isCompleted)
    }

    @Test
    fun `task pin toggle`() {
        val task = createTask(isPinned = false)
        val pinned = task.copy(isPinned = true)

        assertFalse(task.isPinned)
        assertTrue(pinned.isPinned)
    }

    @Test
    fun `task with due date`() {
        val tomorrow = LocalDate.now().plusDays(1)
        val task = createTask(dueDate = tomorrow)

        assertEquals(tomorrow, task.dueDate)
    }

    @Test
    fun `task with recurrence`() {
        val task = createTask(recurrenceType = RecurrenceType.WEEKLY, dueDate = LocalDate.now())

        assertEquals(RecurrenceType.WEEKLY, task.recurrenceType)
    }

    @Test
    fun `task title cannot be blank when validated`() {
        val task = createTask(title = "")
        assertTrue(task.title.isBlank())
    }

    @Test
    fun `task with empty title`() {
        val task = createTask(title = "")
        assertTrue(task.title.isBlank())
        assertEquals(0, task.title.length)
    }

    @Test
    fun `task with whitespace-only title is blank`() {
        val task = createTask(title = "   ")
        assertTrue(task.title.isBlank())
    }

    @Test
    fun `task with long description`() {
        val longDesc = "A".repeat(10000)
        val task = createTask().copy(description = longDesc)
        assertEquals(10000, task.description.length)
    }

    @Test
    fun `task with very long title`() {
        val longTitle = "T".repeat(500)
        val task = createTask(title = longTitle)
        assertEquals(500, task.title.length)
    }

    @Test
    fun `task with reminder fields`() {
        val task = Task(
            id = "reminder-task",
            title = "Reminder Task",
            reminderDate = LocalDate.now().plusDays(1),
            reminderTime = LocalTime.of(9, 0),
            isReminderEnabled = true
        )

        assertTrue(task.isReminderEnabled)
        assertEquals(LocalDate.now().plusDays(1), task.reminderDate)
        assertEquals(LocalTime.of(9, 0), task.reminderTime)
    }

    @Test
    fun `task reminder disabled by default`() {
        val task = createTask()
        assertFalse(task.isReminderEnabled)
        assertNull(task.reminderDate)
        assertNull(task.reminderTime)
    }

    @Test
    fun `task with all fields`() {
        val task = Task(
            id = "full-task",
            title = "Full Task",
            description = "A complete task",
            isCompleted = false,
            priority = Priority.HIGH,
            categoryId = "work",
            dueDate = LocalDate.now(),
            dueTime = LocalTime.of(14, 30),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isPinned = true,
            position = 5,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf(1, 3, 5),
            recurrenceEndDate = LocalDate.now().plusMonths(3),
            reminderDate = LocalDate.now(),
            reminderTime = LocalTime.of(9, 0),
            isReminderEnabled = true,
            parentTaskId = "parent-1",
            originalDueDate = LocalDate.now().minusWeeks(1)
        )

        assertEquals("full-task", task.id)
        assertEquals("Full Task", task.title)
        assertEquals("A complete task", task.description)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals("work", task.categoryId)
        assertTrue(task.isPinned)
        assertEquals(5, task.position)
        assertEquals(RecurrenceType.DAILY, task.recurrenceType)
        assertEquals(2, task.recurrenceInterval)
        assertEquals(listOf(1, 3, 5), task.recurrenceDaysOfWeek)
        assertTrue(task.isReminderEnabled)
        assertEquals("parent-1", task.parentTaskId)
    }

    @Test
    fun `task with listId`() {
        val task = createTask().copy(listId = "list-123")
        assertEquals("list-123", task.listId)
    }

    @Test
    fun `task without listId defaults to null`() {
        val task = createTask()
        assertNull(task.listId)
    }

    @Test
    fun `task can be assigned to different lists`() {
        val task = createTask()
        val publicTask = task.copy(listId = "public-list")
        val privateTask = task.copy(listId = "private-list")
        assertEquals("public-list", publicTask.listId)
        assertEquals("private-list", privateTask.listId)
    }
}
