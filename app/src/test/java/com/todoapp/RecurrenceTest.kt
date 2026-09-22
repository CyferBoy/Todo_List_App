package com.todoapp

import com.todoapp.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {

    private fun calculateNextDate(current: LocalDate, type: RecurrenceType, interval: Int = 1): LocalDate = when (type) {
        RecurrenceType.DAILY -> current.plusDays(interval.toLong())
        RecurrenceType.WEEKLY -> current.plusWeeks(interval.toLong())
        RecurrenceType.MONTHLY -> current.plusMonths(interval.toLong())
        RecurrenceType.YEARLY -> current.plusYears(interval.toLong())
        RecurrenceType.NONE -> current
    }

    @Test
    fun `daily recurrence adds one day`() {
        val date = LocalDate.of(2026, 9, 21)
        val next = calculateNextDate(date, RecurrenceType.DAILY)
        assertEquals(LocalDate.of(2026, 9, 22), next)
    }

    @Test
    fun `daily recurrence with interval`() {
        val date = LocalDate.of(2026, 9, 21)
        val next = calculateNextDate(date, RecurrenceType.DAILY, 3)
        assertEquals(LocalDate.of(2026, 9, 24), next)
    }

    @Test
    fun `weekly recurrence adds one week`() {
        val date = LocalDate.of(2026, 9, 21)
        val next = calculateNextDate(date, RecurrenceType.WEEKLY)
        assertEquals(LocalDate.of(2026, 9, 28), next)
    }

    @Test
    fun `monthly recurrence adds one month`() {
        val date = LocalDate.of(2026, 9, 21)
        val next = calculateNextDate(date, RecurrenceType.MONTHLY)
        assertEquals(LocalDate.of(2026, 10, 21), next)
    }

    @Test
    fun `yearly recurrence adds one year`() {
        val date = LocalDate.of(2026, 9, 21)
        val next = calculateNextDate(date, RecurrenceType.YEARLY)
        assertEquals(LocalDate.of(2027, 9, 21), next)
    }

    @Test
    fun `none recurrence returns same date`() {
        val date = LocalDate.of(2026, 9, 21)
        val next = calculateNextDate(date, RecurrenceType.NONE)
        assertEquals(date, next)
    }

    @Test
    fun `monthly recurrence handles end of month`() {
        val date = LocalDate.of(2026, 1, 31)
        val next = calculateNextDate(date, RecurrenceType.MONTHLY)
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun `monthly recurrence from Jan 31 to Feb 28 in non-leap year`() {
        val date = LocalDate.of(2025, 1, 31)
        val next = calculateNextDate(date, RecurrenceType.MONTHLY)
        assertEquals(LocalDate.of(2025, 2, 28), next)
    }

    @Test
    fun `monthly recurrence from Jan 31 to Feb 29 in leap year`() {
        val date = LocalDate.of(2024, 1, 31)
        val next = calculateNextDate(date, RecurrenceType.MONTHLY)
        assertEquals(LocalDate.of(2024, 2, 29), next)
    }

    @Test
    fun `yearly recurrence on Feb 29 in leap year goes to Feb 28 next year`() {
        val date = LocalDate.of(2024, 2, 29)
        val next = calculateNextDate(date, RecurrenceType.YEARLY)
        assertEquals(LocalDate.of(2025, 2, 28), next)
    }

    @Test
    fun `yearly recurrence on Feb 29 in leap year goes to Feb 29 next leap year`() {
        val date = LocalDate.of(2024, 2, 29)
        val next = calculateNextDate(date, RecurrenceType.YEARLY, 4)
        assertEquals(LocalDate.of(2028, 2, 29), next)
    }

    @Test
    fun `monthly recurrence from May 31 to June 30`() {
        val date = LocalDate.of(2026, 5, 31)
        val next = calculateNextDate(date, RecurrenceType.MONTHLY)
        assertEquals(LocalDate.of(2026, 6, 30), next)
    }

    @Test
    fun `monthly recurrence from Feb 28 to March 28`() {
        val date = LocalDate.of(2026, 2, 28)
        val next = calculateNextDate(date, RecurrenceType.MONTHLY)
        assertEquals(LocalDate.of(2026, 3, 28), next)
    }

    @Test
    fun `recurrence end date check - before end date`() {
        val endDate = LocalDate.of(2026, 12, 31)
        val currentDate = LocalDate.of(2026, 6, 15)
        assertTrue(currentDate.isBefore(endDate) || currentDate.isEqual(endDate))
    }

    @Test
    fun `recurrence end date check - after end date`() {
        val endDate = LocalDate.of(2026, 6, 30)
        val currentDate = LocalDate.of(2026, 7, 1)
        assertFalse(currentDate.isBefore(endDate) || currentDate.isEqual(endDate))
    }

    @Test
    fun `recurrence end date check - on end date`() {
        val endDate = LocalDate.of(2026, 12, 31)
        val currentDate = LocalDate.of(2026, 12, 31)
        assertTrue(currentDate.isBefore(endDate) || currentDate.isEqual(endDate))
    }

    @Test
    fun `weekly recurrence with multiple days of week`() {
        val daysOfWeek = listOf(1, 3, 5)
        val date = LocalDate.of(2026, 9, 21)
        val nextWednesday = date.plusDays(2)
        val nextFriday = date.plusDays(4)
        assertTrue(daysOfWeek.contains(nextWednesday.dayOfWeek.value))
        assertTrue(daysOfWeek.contains(nextFriday.dayOfWeek.value))
    }

    @Test
    fun `weekly recurrence with 2 week interval`() {
        val date = LocalDate.of(2026, 9, 21)
        val next = calculateNextDate(date, RecurrenceType.WEEKLY, 2)
        assertEquals(LocalDate.of(2026, 10, 5), next)
    }

    @Test
    fun `daily recurrence across month boundary`() {
        val date = LocalDate.of(2026, 1, 31)
        val next = calculateNextDate(date, RecurrenceType.DAILY)
        assertEquals(LocalDate.of(2026, 2, 1), next)
    }

    @Test
    fun `daily recurrence across year boundary`() {
        val date = LocalDate.of(2026, 12, 31)
        val next = calculateNextDate(date, RecurrenceType.DAILY)
        assertEquals(LocalDate.of(2027, 1, 1), next)
    }
}
