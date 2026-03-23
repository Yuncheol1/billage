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

    data object ContractSpecialTerms : AppDestination {
        override val route: String = "contract_special_terms"
        const val routeWithArg = "contract_special_terms/{contractSeq}"
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
                // BottomBar에서 Home 이동 시, 그래프의 원래 시작점(보통 Home)까지 스택 정리
                popUpTo(AppDestination.Home.route) {
                    saveState = false
                    inclusive = false
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToContract(role: ContractRole = ContractRole.CREDITOR) {
        navController.navigate("${AppDestination.Contract.route}/${role.name}") {
            // 이전 계약 과정(contractComplete, specialTerms 포함)이 백스택에 남지 않도록
            // saveState=false, restoreState 제거 → 항상 새 계약 1단계부터 시작
            popUpTo(AppDestination.Home.route) { saveState = false }
            launchSingleTop = false
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

    /**
     * ContractComplete → ChatList 전용.
     * 일반 navigateToChatList()는 saveState=true로 계약 플로우 상태를 저장하는데,
     * 이후 홈 버튼 클릭 시 restoreState=true가 계약 플로우를 복원해
     * contractComplete로 돌아가는 문제가 생김.
     * saveState=false로 계약 플로우 상태를 저장하지 않아 해결.
     */
    fun navigateToChatListFromContract() {
        navController.navigate(AppDestination.ChatList.route) {
            popUpTo(AppDestination.Home.route) {
                saveState = false  // 계약 플로우 상태 저장 안 함 → 홈 버튼 시 복원 방지
            }
            launchSingleTop = true
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

    /**
     * ContractComplete → Home 전용.
     * navigate() 기반 navigateToHome()은 launchSingleTop + saveState/restoreState 조합으로
     * ContractComplete처럼 깊은 백스택에서는 no-op이 되는 경우가 있어
     * popBackStack()으로 직접 되감는 방식을 사용.
     */
    fun popToHome() {
        val popped = navController.popBackStack(
            route = AppDestination.Home.route,
            inclusive = false
        )
        if (!popped) {
            // Home이 백스택에 없는 예외 상황 → 강제로 Home 이동
            navController.navigate(AppDestination.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    fun navigateToContractComplete() {
        navController.navigate(AppDestination.ContractComplete.route) {
            launchSingleTop = true
        }
    }

    fun navigateToSpecialTerms(contractSeq: Int = -1) {
        navController.navigate("${AppDestination.ContractSpecialTerms.route}/$contractSeq") {
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