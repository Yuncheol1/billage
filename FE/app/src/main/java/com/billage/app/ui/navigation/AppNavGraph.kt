package com.billage.app.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.billage.app.ui.auth.ConditionsOfUseScreen
import com.billage.app.ui.auth.LoginScreen
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
            onContractClick = { /* TODO: 계약 상세 화면으로 이동 */ },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { /* TODO: 계약 작성 화면으로 이동 */ },
            onActivityClick = { /* TODO: 전체 내역 화면으로 이동 */ }
        )
    }
}
