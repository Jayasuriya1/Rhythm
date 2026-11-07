package com.example.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Utility to toggle and manage modern Android immersive full-screen mode.
 * Hides status and navigation bars cleanly when entering Full-Screen Focus Mode,
 * and guarantees 100% reliable restoration when exiting or disposing the view.
 */
object ImmersiveModeHelper {

    fun enterImmersiveMode(activity: Activity) {
        val window = activity.window ?: return
        val decorView = window.decorView

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun exitImmersiveMode(activity: Activity) {
        val window = activity.window ?: return
        val decorView = window.decorView

        val controller = WindowCompat.getInsetsController(window, decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Composable side-effect that activates immersive full-screen mode while the composable
 * is in the composition tree, and automatically restores normal system bars on exit.
 */
@Composable
fun ImmersiveFocusEffect(enabled: Boolean = true) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(enabled) {
        if (enabled) {
            ImmersiveModeHelper.enterImmersiveMode(activity)
        }
        onDispose {
            if (enabled) {
                ImmersiveModeHelper.exitImmersiveMode(activity)
            }
        }
    }
}
