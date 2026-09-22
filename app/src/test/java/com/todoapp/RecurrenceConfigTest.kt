package com.todoapp

import com.todoapp.domain.model.RecurrenceType
import com.todoapp.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceConfigTest {

    @Test
    fun `weekly days persist`() {
        val task = Task(
            id = "t1", title = "Weekly",
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceDaysOfWeek = listOf(1, 3, 5)
        )
        assertEquals(listOf(1, 3, 5), task.recurrenceDaysOfWeek)
    }

    @Test
    fun `end date persists`() {
        val endDate = LocalDate.of(2026, 12, 31)
        val task = Task(
            id = "t1", title = "Task",
            recurrenceType = RecurrenceType.DAILY,
            recurrenceEndDate = endDate
        )
        assertEquals(endDate, task.recurrenceEndDate)
    }

    @Test
    fun `editing recurrence preserves configuration`() {
        val task = Task(
            id = "t1", title = "Task",
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf(1, 5),
            recurrenceEndDate = LocalDate.of(2026, 6, 30)
        )
        val edited = task.copy(recurrenceInterval = 3)
        assertEquals(RecurrenceType.WEEKLY, edited.recurrenceType)
        assertEquals(3, edited.recurrenceInterval)
        assertEquals(listOf(1, 5), edited.recurrenceDaysOfWeek)
        assertEquals(LocalDate.of(2026, 6, 30), edited.recurrenceEndDate)
    }

    @Test
    fun `interval defaults to 1`() {
        val task = Task(id = "t1", title = "Task", recurrenceType = RecurrenceType.DAILY)
        assertEquals(1, task.recurrenceInterval)
    }

    @Test
    fun `days of week defaults to empty`() {
        val task = Task(id = "t1", title = "Task", recurrenceType = RecurrenceType.WEEKLY)
        assertTrue(task.recurrenceDaysOfWeek.isEmpty())
    }

    @Test
    fun `end date defaults to null`() {
        val task = Task(id = "t1", title = "Task", recurrenceType = RecurrenceType.DAILY)
        assertNull(task.recurrenceEndDate)
    }
}
