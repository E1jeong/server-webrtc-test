package com.sumas.operator

import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.LogDirection
import com.sumas.operator.model.Peer
import com.sumas.operator.model.PeerType
import com.sumas.operator.signaling.SignalingMessage
import com.sumas.operator.state.DesktopOperatorManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopOperatorManagerTest {

    @Test
    fun testManagerUpdatesUrlAndOperatorId() {
        val manager = DesktopOperatorManager()
        assertEquals("ws://localhost:8080/ws", manager.state.value.serverUrl)
        assertEquals("operator-test-01", manager.state.value.operatorId)

        manager.updateServerUrl("ws://192.168.0.50:8080/ws")
        manager.updateOperatorId("custom-op")

        assertEquals("ws://192.168.0.50:8080/ws", manager.state.value.serverUrl)
        assertEquals("custom-op", manager.state.value.operatorId)
    }

    @Test
    fun testSignalingListenerCallbacksTriggerStateUpdates() {
        val manager = DesktopOperatorManager()

        // 1. Connection status change
        manager.onConnectionStatusChanged(ConnectionStatus.CONNECTING)
        assertEquals(ConnectionStatus.CONNECTING, manager.state.value.connectionStatus)

        // 2. Registered message received
        val registered = SignalingMessage.RegisteredMessage(
            peerId = "operator-test-01",
            peers = listOf(
                Peer("device-01", PeerType.DEVICE),
                Peer("device-02", PeerType.DEVICE)
            )
        )
        manager.onMessageReceived(registered)
        assertEquals(ConnectionStatus.REGISTERED, manager.state.value.connectionStatus)
        assertEquals(2, manager.state.value.devices.size)

        // 3. Log event
        manager.onLog(LogDirection.INFO, "Test log message")
        assertEquals(1, manager.state.value.logs.size)
        assertEquals("Test log message", manager.state.value.logs.first().payload)
        assertEquals(LogDirection.INFO, manager.state.value.logs.first().direction)

        // 4. Clear logs
        manager.clearLogs()
        assertTrue(manager.state.value.logs.isEmpty())
    }

    @Test
    fun testInviteAndHangupTransitions() {
        val manager = DesktopOperatorManager()
        manager.onConnectionStatusChanged(ConnectionStatus.REGISTERED)
        manager.onMessageReceived(
            SignalingMessage.RegisteredMessage(
                peerId = "operator-test-01",
                peers = listOf(Peer("device-01", PeerType.DEVICE))
            )
        )

        // Invite
        manager.invite("device-01")
        val calling = assertIs<CallState.Calling>(manager.state.value.callState)
        assertEquals("device-01", calling.targetPeerId)

        // Accept
        manager.onMessageReceived(
            SignalingMessage.CallAcceptMessage(
                callId = calling.callId,
                from = "device-01",
                to = "operator-test-01"
            )
        )
        val inCall = assertIs<CallState.InCall>(manager.state.value.callState)
        assertEquals("device-01", inCall.peerId)

        // Hangup
        manager.hangup()
        assertEquals(CallState.Idle, manager.state.value.callState)
        assertEquals("통화 종료됨", manager.state.value.callStatusMessage)
    }
}
