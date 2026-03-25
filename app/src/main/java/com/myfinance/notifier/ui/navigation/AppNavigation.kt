package com.myfinance.notifier.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.myfinance.notifier.ui.screens.LogScreen
import com.myfinance.notifier.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Settings : Screen("settings")
    data object Log : Screen("log")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Settings.route
    ) {
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(Screen.Log.route) {
            LogScreen()
        }
    }
}
