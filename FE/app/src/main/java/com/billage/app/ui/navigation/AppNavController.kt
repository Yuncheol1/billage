package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

sealed interface AppDestination {
    val route: String

    data object Login : AppDestination {
        override val route: String = "login"
    }

    data object ConditionsOfUse : AppDestination {
        override val route: String = "conditions_of_use"
    }
}

class AppNavController(
    val navController: NavHostController
) {
    fun navigateToLogin() {
        navController.navigate(AppDestination.Login.route) {
            launchSingleTop = true
        }
    }

    fun navigateToConditionsOfUse() {
        navController.navigate(AppDestination.ConditionsOfUse.route) {
            launchSingleTop = true
        }
    }

    fun popBackStack() {
        navController.popBackStack()
    }
}

@Composable
fun rememberAppNavController(
    navController: NavHostController = rememberNavController()
): AppNavController = remember(navController) {
    AppNavController(navController)
}
