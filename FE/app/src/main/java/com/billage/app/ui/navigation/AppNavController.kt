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

    data object Home : AppDestination {
        override val route: String = "home"
    }

    data object Contract : AppDestination {
        override val route: String = "contract"
    }

    data object ChatList : AppDestination {
        override val route: String = "chat_list"
    }

    data object ChatRoom : AppDestination {
        override val route: String = "chat_room"
    }

    data object Mypage : AppDestination {
        override val route: String = "mypage"
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

    fun navigateToConditionsOfUse(clearBackStack: Boolean = false) {
        navController.navigate(AppDestination.ConditionsOfUse.route) {
            launchSingleTop = true
            if (clearBackStack) {
                popUpTo(AppDestination.Login.route) { inclusive = true }
            }
        }
    }

    fun navigateToHome() {
        navController.navigate(AppDestination.Home.route) {
            launchSingleTop = true
        }
    }

    fun navigateToContract() {
        navController.navigate(AppDestination.Contract.route) {
            launchSingleTop = true
        }
    }

    fun navigateToChatList() {
        navController.navigate(AppDestination.ChatList.route) {
            launchSingleTop = true
        }
    }

    fun navigateToChatRoom(roomId: String? = null) {
        // 나중에 roomId를 argument로 넘기도록 수정할 수 있습니다.
        navController.navigate(AppDestination.ChatRoom.route) {
            launchSingleTop = true
        }
    }

    fun navigateToMypage() {
        navController.navigate(AppDestination.Mypage.route) {
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
