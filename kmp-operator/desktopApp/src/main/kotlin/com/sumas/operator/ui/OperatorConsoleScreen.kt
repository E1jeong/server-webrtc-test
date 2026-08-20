package com.sumas.operator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumas.operator.state.DesktopOperatorManager
import com.sumas.operator.ui.components.CallControlPanel
import com.sumas.operator.ui.components.ConnectionPanel
import com.sumas.operator.ui.components.DeviceListPanel
import com.sumas.operator.ui.components.EventLogPanel
import com.sumas.operator.ui.components.TopBar

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

    MaterialTheme(colorScheme = ConsoleDarkColorScheme) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopBar(status = state.connectionStatus)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ConnectionPanel(
                    serverUrl = state.serverUrl,
                    operatorId = state.operatorId,
                    connectionStatus = state.connectionStatus,
                    onUrlChange = { manager.updateServerUrl(it) },
                    onOperatorIdChange = { manager.updateOperatorId(it) },
                    onConnect = { manager.connect() },
                    onDisconnect = { manager.disconnect() }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DeviceListPanel(
                        devices = state.devices,
                        connectionStatus = state.connectionStatus,
                        callState = state.callState,
                        onInviteDevice = { manager.invite(it) },
                        modifier = Modifier.weight(1f)
                    )

                    CallControlPanel(
                        callState = state.callState,
                        callStatusMessage = state.callStatusMessage,
                        onHangup = { manager.hangup() },
                        modifier = Modifier.weight(1f)
                    )
                }

                EventLogPanel(
                    logs = state.logs,
                    onClearLogs = { manager.clearLogs() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
