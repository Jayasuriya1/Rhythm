package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Finds the nearest [Activity] from a given [Context].
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Manages the window's FLAG_KEEP_SCREEN_ON flag safely according to active timer state and user preference.
 * - Applies FLAG_KEEP_SCREEN_ON to the window only while enabled and timer is running.
 * - Removes the flag immediately on disposal, pause, stop, or when setting is toggled off.
 * - Does not hold a continuous wake lock or modify system screen timeout settings.
 */
@Composable
fun KeepScreenOnEffect(enabled: Boolean, isTimerActive: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled, isTimerActive) {
        val window = context.findActivity()?.window
        if (enabled && isTimerActive) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
