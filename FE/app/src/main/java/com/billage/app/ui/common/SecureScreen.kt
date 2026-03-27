package com.billage.app.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

@Composable
fun SecureScreen(
    content: @Composable () -> Unit
) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            WindowSecureFlagManager.acquire(window)
        }

        onDispose {
            if (window != null) {
                WindowSecureFlagManager.release(window)
            }
        }
    }

    content()
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private object WindowSecureFlagManager {
    private val secureWindowRefs = WeakHashMap<Window, Int>()

    fun acquire(window: Window) {
        val refCount = secureWindowRefs[window] ?: 0
        if (refCount == 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        secureWindowRefs[window] = refCount + 1
    }

    fun release(window: Window) {
        val refCount = secureWindowRefs[window] ?: return
        if (refCount <= 1) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            secureWindowRefs.remove(window)
        } else {
            secureWindowRefs[window] = refCount - 1
        }
    }
}
