package com.todoapp.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.di.AppModule
import com.todoapp.domain.model.Task
import com.todoapp.domain.usecase.task.GetTasksByDateRangeUseCase
import com.todoapp.domain.usecase.task.ToggleTaskCompletionUseCase
import com.todoapp.ui.widget.WidgetHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksForMonth: Map<LocalDate, List<Task>> = emptyMap(),
    val tasksForSelectedDate: List<Task> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val getTasksByDateRange = AppModule.provideGetTasksByDateRangeUseCase()
    private val toggleCompletion = AppModule.provideToggleTaskCompletionUseCase()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth()
    }

    fun onMonthChange(month: YearMonth) {
        _uiState.value = _uiState.value.copy(currentMonth = month)
        loadMonth()
    }

    fun onDateSelect(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            tasksForSelectedDate = _uiState.value.tasksForMonth[date] ?: emptyList()
        )
    }

    private fun loadMonth() {
        viewModelScope.launch {
            val month = _uiState.value.currentMonth
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            getTasksByDateRange(start, end).collect { tasks ->
                val tasksByDate = tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! }
                _uiState.value = _uiState.value.copy(
                    tasksForMonth = tasksByDate,
                    tasksForSelectedDate = tasksByDate[_uiState.value.selectedDate] ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun toggleTaskCompletion(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            toggleCompletion(taskId, completed)
            WidgetHelper.refreshWidget(getApplication())
        }
    }
}
