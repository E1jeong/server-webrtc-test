package com.sumas.operator

import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.LogDirection
import com.sumas.operator.model.Peer
import com.sumas.operator.model.PeerType
import com.sumas.operator.signaling.SignalingMessage
import com.sumas.operator.state.OperatorAction
import com.sumas.operator.state.OperatorReducer
import com.sumas.operator.state.OperatorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OperatorReducerTest {

    @Test
    fun testInitialStateAndFieldUpdates() {
        val initial = OperatorState()
        assertEquals("operator-test-01", initial.operatorId)
        assertEquals(ConnectionStatus.DISCONNECTED, initial.connectionStatus)
        assertEquals(CallState.Idle, initial.callState)
        assertTrue(initial.peers.isEmpty())
        assertTrue(initial.devices.isEmpty())

        val updatedUrl = OperatorReducer.reduce(initial, OperatorAction.UpdateServerUrl("ws://192.168.0.10:8080/ws"))
        assertEquals("ws://192.168.0.10:8080/ws", updatedUrl.serverUrl)

        val updatedId = OperatorReducer.reduce(initial, OperatorAction.UpdateOperatorId("operator-custom"))
        assertEquals("operator-custom", updatedId.operatorId)
    }

    @Test
    fun testRegisteredAndPeerSync() {
        var state = OperatorState()
        state = OperatorReducer.reduce(state, OperatorAction.UpdateConnectionStatus(ConnectionStatus.CONNECTING))
        assertEquals(ConnectionStatus.CONNECTING, state.connectionStatus)

        val registered = SignalingMessage.RegisteredMessage(
            peerId = "operator-test-01",
            peers = listOf(
                Peer("device-02", PeerType.DEVICE),
                Peer("operator-other", PeerType.OPERATOR),
                Peer("device-01", PeerType.DEVICE)
            )
        )
        state = OperatorReducer.reduce(state, OperatorAction.ReceiveMessage(registered))
        assertEquals(ConnectionStatus.REGISTERED, state.connectionStatus)
        assertEquals(3, state.peers.size)
        assertEquals(2, state.devices.size)
        // sorted by peerId
        assertEquals("device-01", state.peers[0].peerId)
        assertEquals("device-02", state.peers[1].peerId)

        // New peer online
        val online = SignalingMessage.PeerOnlineMessage("device-03", PeerType.DEVICE)
        state = OperatorReducer.reduce(state, OperatorAction.ReceiveMessage(online))
        assertEquals(4, state.peers.size)
        assertEquals(3, state.devices.size)

        // Peer offline
        val offline = SignalingMessage.PeerOfflineMessage("device-02")
        state = OperatorReducer.reduce(state, OperatorAction.ReceiveMessage(offline))
        assertEquals(3, state.peers.size)
        assertFalse(state.peers.any { it.peerId == "device-02" })
    }

    @Test
    fun testCallLifecycleTransitions() {
        var state = OperatorState(
            operatorId = "operator-01",
            connectionStatus = ConnectionStatus.REGISTERED,
            peers = listOf(Peer("device-01", PeerType.DEVICE))
        )

        // 1. Send invite
        state = OperatorReducer.reduce(state, OperatorAction.SendInvite("device-01"))
        val callingState = assertIs<CallState.Calling>(state.callState)
        assertEquals("device-01", callingState.targetPeerId)
        assertEquals("call-operator-01-1", callingState.callId)
        assertEquals(1, state.callCounter)

        // 2. Accept call
        val accept = SignalingMessage.CallAcceptMessage(
            callId = "call-operator-01-1",
            from = "device-01",
            to = "operator-01"
        )
        state = OperatorReducer.reduce(state, OperatorAction.ReceiveMessage(accept))
        val inCallState = assertIs<CallState.InCall>(state.callState)
        assertEquals("device-01", inCallState.peerId)
        assertEquals("call-operator-01-1", inCallState.callId)
        assertFalse(inCallState.isMicrophoneMuted)

        // 3. Mute microphone
        state = OperatorReducer.reduce(state, OperatorAction.ToggleMicrophoneMute(true))
        val mutedCallState = assertIs<CallState.InCall>(state.callState)
        assertTrue(mutedCallState.isMicrophoneMuted)

        // 4. Remote hangup
        val hangup = SignalingMessage.CallHangupMessage(
            callId = "call-operator-01-1",
            from = "device-01",
            to = "operator-01"
        )
        state = OperatorReducer.reduce(state, OperatorAction.ReceiveMessage(hangup))
        assertEquals(CallState.Idle, state.callState)
    }

    @Test
    fun testCallRejectTransition() {
        var state = OperatorState(
            operatorId = "operator-01",
            connectionStatus = ConnectionStatus.REGISTERED
        )
        state = OperatorReducer.reduce(state, OperatorAction.SendInvite("device-01"))
        assertIs<CallState.Calling>(state.callState)

        val reject = SignalingMessage.CallRejectMessage(
            callId = "call-operator-01-1",
            from = "device-01",
            to = "operator-01"
        )
        state = OperatorReducer.reduce(state, OperatorAction.ReceiveMessage(reject))
        assertEquals(CallState.Idle, state.callState)
    }

    @Test
    fun testTargetPeerOfflineDuringCallCleansUp() {
        var state = OperatorState(
            operatorId = "operator-01",
            connectionStatus = ConnectionStatus.REGISTERED,
            peers = listOf(Peer("device-01", PeerType.DEVICE))
        )
        state = OperatorReducer.reduce(state, OperatorAction.SendInvite("device-01"))
        state = OperatorReducer.reduce(
            state,
            OperatorAction.ReceiveMessage(
                SignalingMessage.CallAcceptMessage("call-operator-01-1", from = "device-01")
            )
        )
        assertIs<CallState.InCall>(state.callState)

        // Device goes offline suddenly
        state = OperatorReducer.reduce(state, OperatorAction.ReceiveMessage(SignalingMessage.PeerOfflineMessage("device-01")))
        assertEquals(CallState.Idle, state.callState)
        assertTrue(state.peers.isEmpty())
    }

    @Test
    fun testSocketDisconnectCleansUpCallAndPeers() {
        var state = OperatorState(
            operatorId = "operator-01",
            connectionStatus = ConnectionStatus.REGISTERED,
            peers = listOf(Peer("device-01", PeerType.DEVICE))
        )
        state = OperatorReducer.reduce(state, OperatorAction.SendInvite("device-01"))

        state = OperatorReducer.reduce(state, OperatorAction.UpdateConnectionStatus(ConnectionStatus.DISCONNECTED))
        assertEquals(ConnectionStatus.DISCONNECTED, state.connectionStatus)
        assertEquals(CallState.Idle, state.callState)
        assertTrue(state.peers.isEmpty())
    }

    @Test
    fun testLogsFifoLimit() {
        var state = OperatorState()
        for (i in 1..100) {
            state = OperatorReducer.reduce(
                state,
                OperatorAction.AddLog(
                    time = "10:00:00",
                    direction = LogDirection.INFO,
                    payload = "Log entry #$i"
                )
            )
        }

        assertEquals(80, state.logs.size)
        assertEquals(100L, state.logCounter)
        assertEquals("Log entry #100", state.logs.first().payload)
        assertEquals("Log entry #21", state.logs.last().payload)

        state = OperatorReducer.reduce(state, OperatorAction.ClearLogs)
        assertTrue(state.logs.isEmpty())
    }
}
