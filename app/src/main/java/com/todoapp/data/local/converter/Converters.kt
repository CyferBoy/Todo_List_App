package com.todoapp.data.local.converter

import androidx.room.TypeConverter
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.RecurrenceType

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = try {
        Priority.valueOf(value)
    } catch (_: Exception) {
        Priority.NONE
    }

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType): String = type.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = try {
        RecurrenceType.valueOf(value)
    } catch (_: Exception) {
        RecurrenceType.NONE
    }
}
