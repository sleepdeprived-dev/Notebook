package com.david.notebook.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.david.notebook.ui.courses.coursesGraph
import com.david.notebook.ui.navigation.Tab
import com.david.notebook.ui.schedule.ScheduleScreen
import com.david.notebook.ui.settings.SettingsScreen
import com.david.notebook.ui.tests.TestsScreen
import com.david.notebook.ui.todo.TodoScreen

private const val SETTINGS_ROUTE = "settings"

@Composable
fun NotebookApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        // Each screen has its own Scaffold + top bar that handles the status bar,
        // so this outer one only reserves room for the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentDestination?.route != SETTINGS_ROUTE) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    // Keep a single copy of each tab on the back stack and remember
                                    // its state (scroll position, open folder) when switching away.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Schedule.route,
            // consumeWindowInsets: the bottom bar already covers the navigation-bar inset,
            // so inner Scaffolds must not add it a second time.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Tab.Schedule.route) {
                ScheduleScreen(onOpenSettings = { navController.navigate(SETTINGS_ROUTE) })
            }
            coursesGraph(route = Tab.Courses.route, navController = navController)
            composable(Tab.Tests.route) { TestsScreen() }
            composable(Tab.Todo.route) { TodoScreen() }
            composable(SETTINGS_ROUTE) { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
