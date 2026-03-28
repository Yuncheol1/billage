package com.billage.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import android.view.WindowManager

@Composable
fun AppNavHost(
    startDestination: String = AppDestination.Login.route,
    isHomeScreenProtectionEnabled: Boolean = true,
    onHomeScreenProtectionChanged: (Boolean) -> Unit = {},
    pendingNotificationTarget: NotificationNavigationTarget? = null,
    onNotificationTargetConsumed: () -> Unit = {}
) {
    key(startDestination) {
        val appNavController = rememberAppNavController()
        val context = LocalContext.current
        val activity = context as? FragmentActivity
        val navBackStackEntry = appNavController.navController.currentBackStackEntryAsState()
        val isHomeRoute = navBackStackEntry.value?.destination?.route == AppDestination.Home.route
        val shouldProtectHome = isHomeRoute && isHomeScreenProtectionEnabled

        DisposableEffect(activity, shouldProtectHome) {
            val window = activity?.window
            if (shouldProtectHome) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            onDispose {
                if (shouldProtectHome) {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

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
            appNavGraph(
                appNavController = appNavController,
                isHomeScreenProtectionEnabled = isHomeScreenProtectionEnabled,
                onHomeScreenProtectionChanged = onHomeScreenProtectionChanged
            )
        }
    }
}
