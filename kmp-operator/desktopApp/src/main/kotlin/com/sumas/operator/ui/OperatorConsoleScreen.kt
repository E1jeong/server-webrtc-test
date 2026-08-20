package com.sumas.operator.ui

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumas.operator.state.DesktopOperatorManager
import com.sumas.operator.ui.components.ConnectionPanel
import com.sumas.operator.ui.components.DeviceListPanel
import com.sumas.operator.ui.components.EventLogPanel
import com.sumas.operator.ui.components.TopBar
import com.sumas.operator.ui.components.VideoStagePanel

private val ConsoleDarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    surface = Color(0xFF1E222B),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF262C38),
    onSurfaceVariant = Color(0xFFB0BEC5),
    background = Color(0xFF13171F),
    onBackground = Color(0xFFECEFF1)
)

@Composable
fun OperatorConsoleScreen(
    manager: DesktopOperatorManager,
    modifier: Modifier = Modifier
) {
    val state by manager.state.collectAsState()
    val videoFrame by manager.remoteVideoFrame.collectAsState()
    var isEventLogVisible by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = ConsoleDarkColorScheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    status = state.connectionStatus,
                    eventLogCount = state.logs.size,
                    onOpenEventLog = { isEventLogVisible = true }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Main Workspace: Left Video Stage + Right (Connection + Devices)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VideoStagePanel(
                            videoFrame = videoFrame,
                            callState = state.callState,
                            callStatusMessage = state.callStatusMessage,
                            onHangup = { manager.hangup() },
                            onToggleMicrophoneMute = { manager.toggleMicrophoneMute() },
                            modifier = Modifier
                                .weight(1.35f)
                                .fillMaxHeight()
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ConnectionPanel(
                                serverUrl = state.serverUrl,
                                operatorId = state.operatorId,
                                connectionStatus = state.connectionStatus,
                                onUrlChange = { manager.updateServerUrl(it) },
                                onOperatorIdChange = { manager.updateOperatorId(it) },
                                onConnect = { manager.connect() },
                                onDisconnect = { manager.disconnect() },
                                modifier = Modifier.fillMaxWidth()
                            )

                            DeviceListPanel(
                                devices = state.devices,
                                connectionStatus = state.connectionStatus,
                                callState = state.callState,
                                onInviteDevice = { manager.invite(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isEventLogVisible,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(420.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 12.dp
                ) {
                    EventLogPanel(
                        logs = state.logs,
                        onClearLogs = { manager.clearLogs() },
                        onClose = { isEventLogVisible = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
