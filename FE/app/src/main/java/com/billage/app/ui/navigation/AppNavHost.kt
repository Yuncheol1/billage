package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation.compose.NavHost

@Composable
fun AppNavHost(startDestination: String = AppDestination.Login.route) {
    key(startDestination) {
        val appNavController = rememberAppNavController()
        NavHost(
            navController = appNavController.navController,
            startDestination = startDestination
        ) {
            appNavGraph(appNavController = appNavController)
        }
    }
}
