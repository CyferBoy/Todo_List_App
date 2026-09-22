package com.todoapp.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoapp.domain.model.ListType
import com.todoapp.domain.model.ThemeMode
import com.todoapp.ui.components.SettingsRow
import com.todoapp.ui.components.SettingsSection
import com.todoapp.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showImportConfirmation by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    val context = LocalContext.current
    var notificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUri = it
            showImportConfirmation = true
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)
            ) {
                SettingsSection(title = "Appearance") {
                    ThemeMode.entries.forEach { mode ->
                        SettingsRow(
                            onClick = { viewModel.setThemeMode(mode) }
                        ) {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(Dimens.SmallGap))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "System default"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                SettingsSection(title = "Notifications") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        SettingsRow {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimens.IconSizeLarge)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SmallGap))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Task Reminders", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (notificationPermissionGranted) "Permission granted" else "Permission required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notificationPermissionGranted,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    } else {
                        SettingsRow {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimens.IconSizeLarge)
                            )
                            Spacer(modifier = Modifier.width(Dimens.SmallGap))
                            Text(
                                text = "Notifications are enabled by default",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SettingsSection(title = "Categories") {
                    uiState.categories.forEach { category ->
                        SettingsRow {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.deleteCategory(category.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Dimens.TinyGap))
                        Text("Add Category")
                    }
                }

                SettingsSection(title = "Sync") {
                    SettingsRow(onClick = { viewModel.syncNow() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = when (uiState.syncStatus) {
                                SyncStatusDisplay.SUCCESS -> MaterialTheme.colorScheme.primary
                                SyncStatusDisplay.FAILED -> MaterialTheme.colorScheme.error
                                SyncStatusDisplay.SYNCING -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(Dimens.IconSizeLarge)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SmallGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sync now", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Last synchronized: ${uiState.lastSyncTime ?: "Never"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = when (uiState.syncStatus) {
                                SyncStatusDisplay.IDLE -> "Offline"
                                SyncStatusDisplay.SYNCING -> "Syncing\u2026"
                                SyncStatusDisplay.SUCCESS -> "Synced"
                                SyncStatusDisplay.FAILED -> "Failed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (uiState.syncStatus) {
                                SyncStatusDisplay.SUCCESS -> MaterialTheme.colorScheme.primary
                                SyncStatusDisplay.FAILED -> MaterialTheme.colorScheme.error
                                SyncStatusDisplay.SYNCING -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                SettingsSection(title = "Lists") {
                    uiState.lists.forEach { list ->
                        SettingsRow {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (list.type == ListType.PUBLIC) {
                                    Text(
                                        text = "Shared",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (list.type != ListType.PUBLIC) {
                                IconButton(onClick = { viewModel.deleteList(list.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showAddListDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Dimens.TinyGap))
                        Text("New List")
                    }
                }

                SettingsSection(title = "Data") {
                    SettingsRow(onClick = { exportLauncher.launch("Todo_Backup.json") }) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.IconSizeLarge)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SmallGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Backup", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Export tasks to a file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SettingsRow(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.IconSizeLarge)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SmallGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Restore", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Import tasks from a backup file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SettingsSection(title = "About") {
                    Text(
                        text = "TodoApp v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.BottomSpacer))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Dimens.ScreenHorizontalPadding)
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addCategory(newCategoryName)
                    newCategoryName = ""
                    showAddCategoryDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newCategoryName = ""
                    showAddCategoryDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddListDialog) {
        AlertDialog(
            onDismissRequest = { showAddListDialog = false },
            title = { Text("New List") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val userId = com.todoapp.data.remote.SupabaseAuthService.getCurrentUserId(context)
                    viewModel.addList(newListName, userId)
                    newListName = ""
                    showAddListDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newListName = ""
                    showAddListDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmation = false
                pendingImportUri = null
            },
            title = { Text("Import Backup") },
            text = { Text("This will replace your current tasks. Are you sure you want to continue?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { viewModel.importData(it) }
                    showImportConfirmation = false
                    pendingImportUri = null
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirmation = false
                    pendingImportUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
