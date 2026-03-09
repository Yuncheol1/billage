package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost

@Composable
fun AppNavHost() {
    val appNavController = rememberAppNavController()
    NavHost(
        navController = appNavController.navController,
        startDestination = AppDestination.Login.route
    ) {
        appNavGraph(appNavController = appNavController)
    }
}
