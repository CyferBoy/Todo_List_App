package com.todoapp.ui.navigation

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Tasks : Screen("tasks")
    data object Calendar : Screen("calendar")
    data object Settings : Screen("settings")
    data object AddTask : Screen("add_task")
    data object EditTask : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: String) = "edit_task/$taskId"
    }
}
