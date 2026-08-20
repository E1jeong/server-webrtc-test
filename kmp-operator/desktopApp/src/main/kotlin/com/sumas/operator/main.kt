package com.sumas.operator

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sumas.operator.state.DesktopOperatorManager
import com.sumas.operator.ui.OperatorConsoleScreen

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1100.dp, 750.dp)
    )
    val manager = remember { DesktopOperatorManager() }

    DisposableEffect(Unit) {
        onDispose {
            manager.cleanup()
        }
    }

    Window(
        onCloseRequest = {
            manager.cleanup()
            exitApplication()
        },
        state = windowState,
        title = "UBio WebRTC - Desktop Operator Console",
    ) {
        OperatorConsoleScreen(manager = manager)
    }
}