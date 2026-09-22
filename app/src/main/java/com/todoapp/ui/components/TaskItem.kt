package com.todoapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todoapp.domain.model.Priority
import com.todoapp.domain.model.Task
import com.todoapp.domain.util.DateUtils
import com.todoapp.ui.theme.BadgeAlpha
import com.todoapp.ui.theme.Dimens
import com.todoapp.ui.theme.PriorityHigh
import com.todoapp.ui.theme.PriorityLow
import com.todoapp.ui.theme.PriorityMedium
import com.todoapp.ui.theme.TaskShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    onToggleCompletion: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.5f else 1f,
        animationSpec = tween(Dimens.AnimationDuration),
        label = "contentAlpha"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleCompletion(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(Dimens.CheckboxTouchTarget)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.isCompleted)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (task.isPinned) {
                        Spacer(modifier = Modifier.width(Dimens.TinyGap))
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(Dimens.IconSizeSmall),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val hasMetadata = task.dueDate != null || task.dueTime != null ||
                        task.priority != Priority.NONE || task.categoryName != null

                if (hasMetadata) {
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.dueDate != null) {
                            val dateText = if (task.dueDate == java.time.LocalDate.now()) {
                                "Today"
                            } else {
                                DateUtils.formatShortDate(task.dueDate)
                            }
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (task.dueTime != null) {
                            if (task.dueDate != null) {
                                MetadataDot()
                            }
                            Text(
                                text = DateUtils.formatTime(task.dueTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (task.priority != Priority.NONE) {
                            if (task.dueDate != null || task.dueTime != null) {
                                MetadataDot()
                            }
                            PriorityBadge(task.priority)
                        }

                        if (task.categoryName != null) {
                            if (task.dueDate != null || task.dueTime != null || task.priority != Priority.NONE) {
                                MetadataDot()
                            }
                            CategoryBadge(task.categoryName, task.categoryColor)
                        }
                    }
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 60.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MetadataDot() {
    Text(
        text = " \u00B7 ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val color = when (priority) {
        Priority.HIGH -> PriorityHigh
        Priority.MEDIUM -> PriorityMedium
        Priority.LOW -> PriorityLow
        Priority.NONE -> return
    }
    Surface(
        modifier = modifier,
        shape = TaskShape.Badge,
        color = color.copy(alpha = BadgeAlpha)
    ) {
        Text(
            text = when (priority) {
                Priority.HIGH -> "High"
                Priority.MEDIUM -> "Medium"
                Priority.LOW -> "Low"
                Priority.NONE -> ""
            },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = Dimens.BadgeHorizontalPadding, vertical = Dimens.BadgeVerticalPadding)
        )
    }
}

@Composable
fun CategoryBadge(name: String, color: Long?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = TaskShape.Badge,
        color = (color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary).copy(alpha = BadgeAlpha)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = Dimens.BadgeHorizontalPadding, vertical = Dimens.BadgeVerticalPadding)
        )
    }
}
