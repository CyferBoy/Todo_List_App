package com.todoapp.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)
    fun formatTime(time: LocalTime): String = time.format(timeFormatter)
    fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)
    fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)
    fun formatMonthYear(date: LocalDate): String = date.format(monthYearFormatter)

    fun localDateToEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun epochMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun todayEpochMillis(): Long = localDateToEpochMillis(LocalDate.now())

    fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    fun getDayOfWeekStrings(): List<String> = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
}
