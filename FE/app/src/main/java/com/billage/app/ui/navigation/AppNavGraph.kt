package com.billage.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.billage.app.ui.auth.ConditionsOfUseScreen
import com.billage.app.ui.auth.LoginScreen
import com.billage.app.ui.auth.MypageScreen
import com.billage.app.ui.contract.ContractScreen
import com.billage.app.ui.home.HomeScreen
import com.billage.app.ui.chat.ChatRoomScreen
import com.billage.app.ui.chat.models.ChatRoomState

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
            onAddContractClick = { appNavController.navigateToContract() },
            onActivityClick = { /* TODO: 전체 내역 화면으로 이동 */ },
            onChatClick = { appNavController.navigateToChatList() },
            onMypageClick = { appNavController.navigateToMypage() }
        )
    }

    composable(route = AppDestination.Contract.route) {
        ContractScreen(
            onNavigateBack = { appNavController.popBackStack() },
            onContractComplete = { /* TODO: 전송 완료 대기 화면으로 이동 */ }
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
        // 프리뷰에서 분리했던 임시 State를 여기서 주입
        com.billage.app.ui.chat.ChatRoomScreen(
            state = ChatRoomState(roomId = "temp_room"),
            onBackClick = { appNavController.popBackStack() },
            onModeChange = {},
            onSendMessage = {},
            onAddMenuClick = {},
            messageListContent = { 
                // 빈 화면 방지용 TODO
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
}
