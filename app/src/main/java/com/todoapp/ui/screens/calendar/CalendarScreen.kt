package com.todoapp.ui.screens.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.domain.util.DateUtils
import com.todoapp.ui.components.EmptyState
import com.todoapp.ui.components.SectionHeader
import com.todoapp.ui.components.TaskItem
import com.todoapp.ui.theme.Dimens
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Calendar") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.onMonthChange(uiState.currentMonth.minusMonths(1))
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    text = DateUtils.formatMonthYear(uiState.currentMonth.atDay(1)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = {
                    viewModel.onMonthChange(uiState.currentMonth.plusMonths(1))
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SmallGap))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.TinyGap))

            val daysInMonth = uiState.currentMonth.lengthOfMonth()
            val firstDayOfWeek = uiState.currentMonth.atDay(1).dayOfWeek.value % 7
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(Dimens.CalendarCellHeight * rows)
            ) {
                items(totalCells) { index ->
                    if (index < firstDayOfWeek) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    } else {
                        val day = index - firstDayOfWeek + 1
                        val date = uiState.currentMonth.atDay(day)
                        val isSelected = date == uiState.selectedDate
                        val isToday = date == LocalDate.now()
                        val hasTasks = uiState.tasksForMonth[date]?.isNotEmpty() == true

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(Dimens.TinyGap)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { viewModel.onDateSelect(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (hasTasks) {
                                    Box(
                                        modifier = Modifier
                                            .size(Dimens.TaskDotSize)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.FieldGap))

            SectionHeader(
                title = DateUtils.formatFullDate(uiState.selectedDate),
                modifier = Modifier.padding(bottom = Dimens.SmallGap)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.animateContentSize()
            ) {
                itemsIndexed(uiState.tasksForSelectedDate, key = { _, task -> task.id }) { index, task ->
                    TaskItem(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task.id, it) },
                        onClick = { onEditTask(task.id) },
                        showDivider = index < uiState.tasksForSelectedDate.size - 1
                    )
                }

                if (uiState.tasksForSelectedDate.isEmpty()) {
                    item {
                        EmptyState(
                            icon = {
                                Icon(
                                    Icons.Outlined.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            title = "No tasks for this date"
                        )
                    }
                }
            }
        }
    }
}
