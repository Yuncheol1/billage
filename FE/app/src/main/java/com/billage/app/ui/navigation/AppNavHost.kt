package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.navigation.compose.NavHost

@Composable
fun AppNavHost(
    startDestination: String = AppDestination.Login.route,
    pendingNotificationTarget: NotificationNavigationTarget? = null,
    onNotificationTargetConsumed: () -> Unit = {}
) {
    key(startDestination) {
        val appNavController = rememberAppNavController()
        LaunchedEffect(pendingNotificationTarget) {
            val target = pendingNotificationTarget ?: return@LaunchedEffect
            when {
                target.contractSeq != null -> appNavController.navigateToContractDetail(target.contractSeq.toString())
                target.openNotifications -> appNavController.navigateToNotifications()
            }
            onNotificationTargetConsumed()
        }
        NavHost(
            navController = appNavController.navController,
            startDestination = startDestination
        ) {
            appNavGraph(appNavController = appNavController)
        }
    }
}
