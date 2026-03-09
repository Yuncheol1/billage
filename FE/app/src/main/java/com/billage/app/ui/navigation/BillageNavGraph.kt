package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.billage.app.ui.home.HomeScreen

@Composable
fun BillageNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ✅ HomeScreen 연결 완료
        composable(route = Screen.Home.route) {
            HomeScreen(
                onContractClick = { contractId ->
                    // TODO: 계약 상세 화면으로 이동 (추후 구현)
                },
                onNotificationClick = {
                    // TODO: 알림 화면으로 이동 (추후 구현)
                },
                onAddContractClick = {
                    // TODO: 계약 작성 화면으로 이동 (추후 구현)
                },
                onActivityClick = {
                    // TODO: 전체 내역 화면으로 이동 (추후 구현)
                }
            )
        }

        // TODO: 앞으로 화면이 추가될 때 여기에 계속 추가.
    }
}