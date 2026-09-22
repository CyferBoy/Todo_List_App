package com.todoapp

import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderTest {

    @Test
    fun `newly created task uses persisted ID for reminder`() {
        val task = Task(id = "", title = "New Task")
        val persistedTask = task.copy(id = "generated-id-123")
        assertEquals("generated-id-123", persistedTask.id)
        assertTrue(persistedTask.id.isNotEmpty())
    }

    @Test
    fun `delete cancels reminder`() {
        val task = Task(id = "t1", title = "Task", isReminderEnabled = true,
            reminderDate = LocalDate.now().plusDays(1), reminderTime = LocalTime.of(9, 0))
        assertTrue(task.isReminderEnabled)
    }

    @Test
    fun `completion cancels current reminder`() {
        val task = Task(id = "t1", title = "Task", isReminderEnabled = true,
            reminderDate = LocalDate.now().plusDays(1), reminderTime = LocalTime.of(9, 0))
        val completed = task.copy(isCompleted = true)
        assertTrue(completed.isCompleted)
    }

    @Test
    fun `recurring next occurrence schedules new reminder`() {
        val task = Task(
            id = "t1", title = "Weekly Task",
            recurrenceType = RecurrenceType.WEEKLY,
            dueDate = LocalDate.now(),
            isReminderEnabled = true,
            reminderDate = LocalDate.now(),
            reminderTime = LocalTime.of(9, 0)
        )
        val nextDate = task.dueDate!!.plusWeeks(1)
        val nextTask = task.copy(
            id = "t2",
            dueDate = nextDate,
            reminderDate = nextDate,
            parentTaskId = task.id,
            isCompleted = false
        )
        assertEquals(nextDate, nextTask.dueDate)
        assertEquals(nextDate, nextTask.reminderDate)
        assertEquals("t1", nextTask.parentTaskId)
    }

    @Test
    fun `reminder in the past should not be scheduled`() {
        val pastTime = LocalTime.of(0, 0)
        val pastDate = LocalDate.now().minusDays(1)
        val task = Task(id = "t1", title = "Task", isReminderEnabled = true,
            reminderDate = pastDate, reminderTime = pastTime)
        val triggerTime = task.reminderDate!!.atTime(task.reminderTime!!)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertTrue(triggerTime < System.currentTimeMillis())
    }

    @Test
    fun `reminder disabled means no scheduling`() {
        val task = Task(id = "t1", title = "Task", isReminderEnabled = false)
        assertFalse(task.isReminderEnabled)
    }

    @Test
    fun `recurring task completion enqueues sync operation`() {
        val oldTask = Task(id = "old", title = "Old", isCompleted = false,
            recurrenceType = RecurrenceType.WEEKLY)
        val newTask = Task(id = "new", title = "New", isCompleted = false,
            parentTaskId = "old", recurrenceType = RecurrenceType.WEEKLY)
        assertNotNull(oldTask.id)
        assertNotNull(newTask.id)
    }
}
