package com.billage.app.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.billage.app.ui.auth.ConditionsOfUseScreen
import com.billage.app.ui.auth.LoginScreen
import com.billage.app.ui.auth.MypageScreen
import com.billage.app.ui.chat.models.ChatRoomState
import com.billage.app.ui.contract.ContractCompleteScreen
import com.billage.app.ui.contract.ContractRole
import com.billage.app.ui.contract.ContractScreen
import com.billage.app.ui.contract.ContractViewModel
import com.billage.app.ui.home.HomeScreen

fun NavGraphBuilder.appNavGraph(
    appNavController: AppNavController
) {
    composable(route = AppDestination.Login.route) {
        LoginScreen(
            onLoginClick = { appNavController.navigateToConditionsOfUse() }
        )
    }

    composable(route = AppDestination.ConditionsOfUse.route) {
        ConditionsOfUseScreen(
            onBackClick = { appNavController.popBackStack() },
            onAgreeClick = { appNavController.navigateToHome() }
        )
    }

    composable(route = AppDestination.Home.route) {
        HomeScreen(
            onContractClick = { appNavController.navigateToMypage() },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { role -> appNavController.navigateToContract(role) },
            onActivityClick = { /* TODO: 전체 내역 화면으로 이동 */ },
            onChatClick = { appNavController.navigateToChatList() },
            onMypageClick = { appNavController.navigateToMypage() }
        )
    }

    composable(route = AppDestination.Contract.routeWithArg) { backStackEntry ->
        val role = backStackEntry.arguments?.getString("role")
            ?.let { ContractRole.valueOf(it) } ?: ContractRole.CREDITOR
        ContractScreen(
            onNavigateBack = { appNavController.popBackStack() },
            onContractComplete = { appNavController.navigateToContractComplete() },
            role = role
        )
    }

    composable(route = AppDestination.ChatList.route) {
        com.billage.app.ui.chat.ChatListScreen(
            onChatRoomClick = { roomId -> appNavController.navigateToChatRoom(roomId) },
            onHomeClick = { appNavController.navigateToHome() },
            onAddContractClick = { appNavController.navigateToContract() },
            onContractClick = { appNavController.navigateToMypage() }
        )
    }

    composable(route = AppDestination.ChatRoom.route) {
        com.billage.app.ui.chat.ChatRoomScreen(
            state = ChatRoomState(roomId = "temp_room"),
            onBackClick = { appNavController.popBackStack() },
            onModeChange = {},
            onSendMessage = {},
            onAddMenuClick = {},
            messageListContent = {
                // TODO: 실제 메시지 목록 연결
            }
        )
    }

    composable(route = AppDestination.Mypage.route) {
        MypageScreen(
            onNavigateHome = { appNavController.navigateToHome() },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { appNavController.navigateToContract() },
            onChatClick = { appNavController.navigateToChatList() }
        )
    }

    composable(route = AppDestination.ContractComplete.route) { entry ->
        // Contract 화면의 ViewModel 공유 — 백스택에 남아있는 Contract 화면의 ViewModel 참조
        val parentEntry = remember(entry) {
            appNavController.navController.getBackStackEntry(AppDestination.Contract.routeWithArg)
        }
        val viewModel: ContractViewModel = viewModel(parentEntry)
        val formState by viewModel.formState.collectAsState()

        ContractCompleteScreen(
            formState = formState,
            onHomeClick = { appNavController.navigateToHome() },
            onChatClick = { appNavController.navigateToChatList() }, // TODO: 생성된 채팅방으로 직접 이동
            role = formState.role
        )
    }
}