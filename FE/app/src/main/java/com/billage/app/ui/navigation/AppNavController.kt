package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.billage.app.ui.contract.ContractRole

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
        const val routeWithArg = "contract/{role}"
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

    data object ContractComplete : AppDestination {
        override val route: String = "contract_complete"
    }

    data object ContractEdit : AppDestination {
        override val route: String = "contract_edit"
        const val routeWithArg = "contract_edit/{contractId}"
    }

    data object ContractDetail : AppDestination {
        override val route: String = "contract_detail"
        const val routeWithArg = "contract_detail/{contractId}"
    }
}

class AppNavController(
    val navController: NavHostController
) {
    fun navigateToLogin(clearBackStack: Boolean = false) {
        navController.navigate(AppDestination.Login.route) {
            launchSingleTop = true
            if (clearBackStack) {
                popUpTo(AppDestination.Home.route) {
                    inclusive = true
                }
            }
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

    fun navigateToHome(clearBackStack: Boolean = false) {
        navController.navigate(AppDestination.Home.route) {
            if (clearBackStack) {
                popUpTo(AppDestination.Login.route) {
                    inclusive = true
                }
            } else {
                popUpTo(AppDestination.Home.route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToContract(role: ContractRole = ContractRole.CREDITOR) {
        navController.navigate("${AppDestination.Contract.route}/${role.name}") {
            popUpTo(AppDestination.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToChatList() {
        navController.navigate(AppDestination.ChatList.route) {
            popUpTo(AppDestination.Home.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToChatRoom(roomId: String? = null, opponentName: String? = null) {
        val route = buildString {
            append(AppDestination.ChatRoom.route)
            if (roomId != null) {
                append("/$roomId")
                if (opponentName != null) {
                    append("/$opponentName")
                }
            }
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateToMypage() {
        navController.navigate(AppDestination.Mypage.route) {
            popUpTo(AppDestination.Home.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun popBackStack() {
        navController.popBackStack()
    }

    fun navigateToContractComplete() {
        navController.navigate(AppDestination.ContractComplete.route) {
            launchSingleTop = true
        }
    }

    fun navigateToContractEdit(contractId: String) {
        navController.navigate("${AppDestination.ContractEdit.route}/$contractId") {
            launchSingleTop = true
        }
    }

    fun navigateToContractDetail(contractId: String) {
        navController.navigate("${AppDestination.ContractDetail.route}/$contractId") {
            launchSingleTop = true
        }
    }
}

@Composable
fun rememberAppNavController(
    navController: NavHostController = rememberNavController()
): AppNavController = remember(navController) {
    AppNavController(navController)
}