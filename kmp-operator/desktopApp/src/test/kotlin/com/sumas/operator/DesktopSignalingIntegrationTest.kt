package com.sumas.operator

import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.PeerType
import com.sumas.operator.signaling.DesktopWebSocketClient
import com.sumas.operator.signaling.SignalingMessage
import com.sumas.operator.state.DesktopOperatorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopSignalingIntegrationTest {

    @Test
    fun testEndToEndSignalingWithServer() = runBlocking {
        val serverUrl = "ws://localhost:8888/ws"

        // 1. Create operator manager
        val operatorManager = DesktopOperatorManager()
        operatorManager.updateServerUrl(serverUrl)
        operatorManager.updateOperatorId("operator-integration-01")

        // 2. Create a mock device client using DesktopWebSocketClient
        val mockDeviceClient = DesktopWebSocketClient()
        var deviceRegistered = false
        var lastDeviceMessage: SignalingMessage? = null

        val deviceListener = object : com.sumas.operator.signaling.SignalingListener {
            override fun onConnectionStatusChanged(status: ConnectionStatus) {
                if (status == ConnectionStatus.REGISTERED) {
                    deviceRegistered = true
                }
            }

            override fun onMessageReceived(message: SignalingMessage) {
                lastDeviceMessage = message
                if (message is SignalingMessage.RegisteredMessage) {
                    deviceRegistered = true
                }
            }

            override fun onLog(direction: com.sumas.operator.model.LogDirection, payload: String) {}
        }

        // Connect mock device
        mockDeviceClient.connect(serverUrl, "device-integration-01", deviceListener, PeerType.DEVICE)

        val connected = withTimeoutOrNull(3000) {
            while (!deviceRegistered) {
                delay(50)
            }
            true
        }

        if (connected != true) {
            println("Signaling server not reachable at $serverUrl, skipping test.")
            mockDeviceClient.disconnect()
            operatorManager.cleanup()
            return@runBlocking
        }

        // 3. Connect operator
        operatorManager.connect()
        val operatorReady = withTimeoutOrNull(3000) {
            while (operatorManager.state.value.connectionStatus != ConnectionStatus.REGISTERED) {
                delay(50)
            }
            true
        }
        assertTrue(operatorReady == true, "Operator should register successfully")

        // Check if device is in operator's device list
        val foundDevice = withTimeoutOrNull(3000) {
            while (!operatorManager.state.value.devices.any { it.peerId == "device-integration-01" }) {
                delay(50)
            }
            true
        }
        assertTrue(foundDevice == true, "Operator should receive device in online peer list")

        // 4. Operator invites device
        operatorManager.invite("device-integration-01")
        assertIs<CallState.Calling>(operatorManager.state.value.callState)

        // Verify device received call.invite
        val receivedInvite = withTimeoutOrNull(3000) {
            while (lastDeviceMessage !is SignalingMessage.CallInviteMessage) {
                delay(50)
            }
            lastDeviceMessage as SignalingMessage.CallInviteMessage
        }
        assertTrue(receivedInvite != null, "Device should receive call.invite")
        assertEquals("device-integration-01", receivedInvite.to)
        assertEquals("operator-integration-01", receivedInvite.from)

        // 5. Device sends call.accept
        mockDeviceClient.send(
            SignalingMessage.CallAcceptMessage(
                callId = receivedInvite.callId,
                to = "operator-integration-01"
            )
        )

        // Verify operator entered InCall
        val inCall = withTimeoutOrNull(3000) {
            while (operatorManager.state.value.callState !is CallState.InCall) {
                delay(50)
            }
            operatorManager.state.value.callState as CallState.InCall
        }
        assertTrue(inCall != null, "Operator should transition to InCall")
        assertEquals("device-integration-01", inCall.peerId)
        assertEquals("통화 연결됨", operatorManager.state.value.callStatusMessage)

        // 6. Device hangs up
        mockDeviceClient.send(
            SignalingMessage.CallHangupMessage(
                callId = inCall.callId,
                to = "operator-integration-01"
            )
        )

        val callEnded = withTimeoutOrNull(3000) {
            while (operatorManager.state.value.callState !is CallState.Idle) {
                delay(50)
            }
            true
        }
        assertTrue(callEnded == true, "Operator should transition to Idle upon remote hangup")
        assertEquals("통화가 종료되었습니다", operatorManager.state.value.callStatusMessage)

        // Cleanup
        mockDeviceClient.disconnect()
        operatorManager.cleanup()
    }
}
