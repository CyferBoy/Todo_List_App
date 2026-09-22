package com.todoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.todoapp.ui.screens.today.TodayScreen
import com.todoapp.ui.screens.tasks.TasksScreen
import com.todoapp.ui.screens.calendar.CalendarScreen
import com.todoapp.ui.screens.settings.SettingsScreen
import com.todoapp.ui.screens.addedit.AddEditTaskScreen

@Composable
fun TodoNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = modifier
    ) {
        composable(Screen.Today.route) {
            TodayScreen(
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onEditTask = { taskId -> navController.navigate(Screen.EditTask.createRoute(taskId)) }
            )
        }
        composable(Screen.Tasks.route) {
            TasksScreen(
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onEditTask = { taskId -> navController.navigate(Screen.EditTask.createRoute(taskId)) }
            )
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onEditTask = { taskId -> navController.navigate(Screen.EditTask.createRoute(taskId)) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(Screen.AddTask.route) {
            AddEditTaskScreen(
                taskId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            AddEditTaskScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
