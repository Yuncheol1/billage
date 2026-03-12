package com.billage.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.billage.app.ui.auth.ConditionsOfUseScreen
import com.billage.app.ui.auth.LoginScreen
import com.billage.app.ui.auth.MypageScreen
import com.billage.app.ui.contract.ContractScreen
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
            onAddContractClick = { appNavController.navigateToContract() },
            onActivityClick = { /* TODO: 전체 내역 화면으로 이동 */ },
            onMypageClick = { appNavController.navigateToMypage() }
        )
    }

    composable(route = AppDestination.Contract.route) {
        ContractScreen(
            onNavigateBack = { appNavController.popBackStack() },
            onContractComplete = { /* TODO: 전송 완료 대기 화면으로 이동 */ }
        )
    }

    composable(route = AppDestination.Mypage.route) {
        MypageScreen(
            onNavigateHome = { appNavController.navigateToHome() },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { appNavController.navigateToContract() }
        )
    }
}
