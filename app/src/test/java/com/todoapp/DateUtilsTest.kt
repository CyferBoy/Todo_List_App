package com.todoapp

import com.todoapp.domain.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateUtilsTest {

    @Test
    fun `formatDate produces correct format`() {
        val date = LocalDate.of(2026, 9, 21)
        val formatted = DateUtils.formatDate(date)
        assertEquals("Sep 21, 2026", formatted)
    }

    @Test
    fun `formatTime produces correct format`() {
        val time = LocalTime.of(14, 30)
        val formatted = DateUtils.formatTime(time)
        assertEquals("2:30 PM", formatted)
    }

    @Test
    fun `formatFullDate produces correct format`() {
        val date = LocalDate.of(2026, 9, 21)
        val formatted = DateUtils.formatFullDate(date)
        assertEquals("Monday, 21 September 2026", formatted)
    }

    @Test
    fun `formatShortDate produces correct format`() {
        val date = LocalDate.of(2026, 9, 21)
        val formatted = DateUtils.formatShortDate(date)
        assertEquals("21 Sep", formatted)
    }

    @Test
    fun `formatMonthYear produces correct format`() {
        val date = LocalDate.of(2026, 9, 1)
        val formatted = DateUtils.formatMonthYear(date)
        assertEquals("September 2026", formatted)
    }

    @Test
    fun `localDateToEpochMillis and back are consistent`() {
        val date = LocalDate.of(2026, 9, 21)
        val millis = DateUtils.localDateToEpochMillis(date)
        val back = DateUtils.epochMillisToLocalDate(millis)
        assertEquals(date, back)
    }

    @Test
    fun `getGreeting returns appropriate greeting`() {
        val greeting = DateUtils.getGreeting()
        assertTrue(
            greeting == "Good morning" ||
            greeting == "Good afternoon" ||
            greeting == "Good evening"
        )
    }

    @Test
    fun `getDayOfWeekStrings has 7 entries`() {
        assertEquals(7, DateUtils.getDayOfWeekStrings().size)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
