package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.billage.app.ui.contract.create.ContractRole
import com.billage.app.ui.contract.list.ContractRoleFilter
import com.billage.app.ui.contract.list.ContractStatusFilter

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
        const val routeWithArg = "contract/{role}?initialName={initialName}"
    }

    data object ChatList : AppDestination {
        override val route: String = "chat_list"
    }

    data object ChatRoom : AppDestination {
        override val route: String = "chat_room"
        const val routeWithArg = "chat_room/{roomId}/{opponentName}/{isNewContract}/{contractSeq}"
    }

    data object Mypage : AppDestination {
        override val route: String = "mypage"
        const val routeWithArgs = "mypage?role={role}&status={status}"
    }

    data object Settings : AppDestination {
        override val route: String = "settings"
    }

    data object ContractComplete : AppDestination {
        override val route: String = "contract_complete"
        const val routeWithArg = "contract_complete/{contractSeq}/{roomId}/{opponentName}"
    }

    data object ContractSpecialTerms : AppDestination {
        override val route: String = "contract_special_terms"
        const val routeWithArg = "contract_special_terms/{contractSeq}/{roomId}/{opponentName}"
    }

    data object ContractEdit : AppDestination {
        override val route: String = "contract_edit"
        const val routeWithArg = "contract_edit/{contractSeq}?roomId={roomId}"
    }

    data object ContractDetail : AppDestination {
        override val route: String = "contract_detail"
        const val routeWithArg = "contract_detail/{contractId}"
    }

    data object Notification : AppDestination {
        override val route: String = "notification"
    }

    data object InsightList : AppDestination {
        override val route: String = "insight_list"
    }

    data object InsightDetail : AppDestination {
        override val route: String = "insight_detail"
        const val routeWithArg = "insight_detail/{insightIndex}"
    }

    data object Account : AppDestination {
        override val route: String = "account"
    }

    data object AccountDetail : AppDestination {
        override val route: String = "account_detail"
        const val routeWithArg = "account_detail/{transactionUniqueNo}"
    }

    data object ContractDocument : AppDestination {
        override val route: String = "contract_document"
        const val routeWithArg = "contract_document/{contractSeq}"
    }

    data object ChatBot : AppDestination {
        override val route: String = "chatbot"
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

    fun navigateToContract(role: ContractRole = ContractRole.CREDITOR, initialName: String? = null) {
        val baseRoute = "${AppDestination.Contract.route}/${role.name}"
        val finalRoute = if (initialName != null) "$baseRoute?initialName=$initialName" else baseRoute
        navController.navigate(finalRoute) {
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

    fun navigateToChatRoom(
        roomId: String,
        opponentName: String,
        isNewContract: Boolean = false,
        contractSeq: Int = -1,
        clearContractFlow: Boolean = false
    ) {
        navController.navigate("${AppDestination.ChatRoom.route}/$roomId/$opponentName/$isNewContract/$contractSeq") {
            launchSingleTop = true
            if (clearContractFlow) {
                // 계약 생성 플로우(Contract, SpecialTerms, Complete)를 모두 스택에서 제거
                // ChatList가 있으면 거기서 멈추고, 없으면 Home까지 정리
                popUpTo(AppDestination.Home.route) { 
                    inclusive = false
                    saveState = false 
                }
            }
        }
    }

    fun navigateToMypage(
        role: ContractRoleFilter? = null,
        status: ContractStatusFilter? = null
    ) {
        val route = buildString {
            append(AppDestination.Mypage.route)
            val params = listOfNotNull(
                role?.let { "role=${it.name}" },
                status?.let { "status=${it.name}" }
            )
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }

        navController.navigate(route) {
            popUpTo(AppDestination.Home.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = role == null && status == null
        }
    }

    fun navigateToSettings() {
        navController.navigate(AppDestination.Settings.route) {
            launchSingleTop = true
        }
    }

    fun navigateToNotifications() {
        navController.navigate(AppDestination.Notification.route) {
            launchSingleTop = true
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

    fun navigateToContractComplete(
        contractSeq: Int = -1,
        roomId: String = "none",
        opponentName: String = "알 수 없음"
    ) {
        navController.navigate("contract_complete/$contractSeq/$roomId/$opponentName") {
            popUpTo(AppDestination.Contract.route) { inclusive = true }
        }
    }

    fun navigateToContractEdit(contractSeq: Int, roomId: String? = null) {
        val route = if (roomId != null) {
            "${AppDestination.ContractEdit.route}/$contractSeq?roomId=$roomId"
        } else {
            "${AppDestination.ContractEdit.route}/$contractSeq"
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateToContractDetail(contractId: String) {
        navController.navigate("${AppDestination.ContractDetail.route}/$contractId") {
            launchSingleTop = true
        }
    }

    fun navigateToSpecialTerms(
        contractSeq: Int = -1,
        roomId: String = "none",
        opponentName: String = "알 수 없음"
    ) {
        navController.navigate("${AppDestination.ContractSpecialTerms.route}/$contractSeq/$roomId/$opponentName") {
            launchSingleTop = true
        }
    }

    fun navigateToInsightList() {
        navController.navigate(AppDestination.InsightList.route) {
            popUpTo(AppDestination.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToInsightDetail(insightIndex: Int) {
        navController.navigate("${AppDestination.InsightDetail.route}/$insightIndex") {
            launchSingleTop = true
        }
    }

    fun navigateToContractDocument(contractSeq: Int) {
        navController.navigate("${AppDestination.ContractDocument.route}/$contractSeq") {
            launchSingleTop = true
        }
    }

    fun navigateToAccount() {
        navController.navigate(AppDestination.Account.route) {
            launchSingleTop = true
        }
    }

    fun navigateToAccountDetail(transactionUniqueNo: Long) {
        navController.navigate("${AppDestination.AccountDetail.route}/$transactionUniqueNo") {
            launchSingleTop = true
        }
    }

    fun navigateToChatBot() {
        navController.navigate(AppDestination.ChatBot.route) {
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
