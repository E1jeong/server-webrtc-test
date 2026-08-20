package com.sumas.operator

import com.sumas.operator.media.FakeDesktopMediaController
import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.Peer
import com.sumas.operator.model.PeerType
import com.sumas.operator.signaling.DesktopWebSocketClient
import com.sumas.operator.signaling.SignalingMessage
import com.sumas.operator.state.DesktopOperatorManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopMediaOrchestrationTest {

    private fun createRegisteredManager(
        fakeMediaController: FakeDesktopMediaController = FakeDesktopMediaController()
    ): DesktopOperatorManager {
        val manager = DesktopOperatorManager(mediaController = fakeMediaController)
        manager.onConnectionStatusChanged(ConnectionStatus.REGISTERED)
        manager.onMessageReceived(
            SignalingMessage.RegisteredMessage(
                peerId = "operator-test-01",
                peers = listOf(Peer("device-01", PeerType.DEVICE))
            )
        )
        return manager
    }

    @Test
    fun testCallAcceptStartsMediaAndEmitsOfferAndIce() {
        val fakeMedia = FakeDesktopMediaController(
            autoGenerateOffer = true,
            autoGenerateIce = true,
            mockOfferSdp = "v=0\r\no=- 12345 offer\r\n",
            mockIceCandidate = "candidate:test-1"
        )
        val manager = createRegisteredManager(fakeMedia)

        // 1. Send invite
        manager.invite("device-01")
        val calling = assertIs<CallState.Calling>(manager.state.value.callState)
        assertEquals("device-01", calling.targetPeerId)
        val callId = calling.callId

        // 2. Receive accept from device
        manager.onMessageReceived(
            SignalingMessage.CallAcceptMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01"
            )
        )

        // 3. Verify media started
        val inCall = assertIs<CallState.InCall>(manager.state.value.callState)
        assertEquals("device-01", inCall.peerId)
        assertEquals(callId, inCall.callId)
        assertTrue(inCall.isMediaReady)
        assertTrue(fakeMedia.isCallActive)
        assertEquals(callId, fakeMedia.activeCallId)
        assertEquals("device-01", fakeMedia.activeTargetDeviceId)
    }

    @Test
    fun testRemoteAnswerAndTrickleIceAreForwardedToMediaController() {
        val fakeMedia = FakeDesktopMediaController(autoGenerateOffer = false, autoGenerateIce = false)
        val manager = createRegisteredManager(fakeMedia)

        manager.invite("device-01")
        val calling = assertIs<CallState.Calling>(manager.state.value.callState)
        val callId = calling.callId

        manager.onMessageReceived(
            SignalingMessage.CallAcceptMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01"
            )
        )

        // Remote answer received
        val answerSdp = "v=0\r\no=- 67890 answer\r\n"
        manager.onMessageReceived(
            SignalingMessage.WebRtcAnswerMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01",
                sdp = answerSdp
            )
        )
        assertEquals(1, fakeMedia.receivedAnswers.size)
        assertEquals(answerSdp, fakeMedia.receivedAnswers.first())

        // Remote ICE candidate received
        manager.onMessageReceived(
            SignalingMessage.WebRtcIceMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01",
                candidate = "candidate:remote-1",
                sdpMid = "0",
                sdpMLineIndex = 0
            )
        )
        assertEquals(1, fakeMedia.receivedCandidates.size)
        assertEquals("candidate:remote-1", fakeMedia.receivedCandidates.first().candidate)
    }

    @Test
    fun testIceCandidateBeforeAnswerIsBufferedAndFlushed() {
        val fakeMedia = FakeDesktopMediaController(autoGenerateOffer = false, autoGenerateIce = false)
        val manager = createRegisteredManager(fakeMedia)

        manager.invite("device-01")
        val calling = assertIs<CallState.Calling>(manager.state.value.callState)
        val callId = calling.callId

        manager.onMessageReceived(
            SignalingMessage.CallAcceptMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01"
            )
        )

        // 1. ICE candidate arrives BEFORE answer
        manager.onMessageReceived(
            SignalingMessage.WebRtcIceMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01",
                candidate = "candidate:early-1",
                sdpMid = "0",
                sdpMLineIndex = 0
            )
        )

        // Should be in pendingIceCandidates, not in receivedCandidates
        assertEquals(1, fakeMedia.pendingIceCandidates.size)
        assertEquals(0, fakeMedia.receivedCandidates.size)
        assertEquals("candidate:early-1", fakeMedia.pendingIceCandidates.first().candidate)

        // 2. Now answer arrives
        manager.onMessageReceived(
            SignalingMessage.WebRtcAnswerMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01",
                sdp = "v=0\r\nanswer\r\n"
            )
        )

        // Pending candidates should now be flushed to receivedCandidates
        assertEquals(0, fakeMedia.pendingIceCandidates.size)
        assertEquals(1, fakeMedia.receivedCandidates.size)
        assertEquals("candidate:early-1", fakeMedia.receivedCandidates.first().candidate)
    }

    @Test
    fun testMicrophoneMuteToggle() {
        val fakeMedia = FakeDesktopMediaController()
        val manager = createRegisteredManager(fakeMedia)

        manager.invite("device-01")
        val calling = assertIs<CallState.Calling>(manager.state.value.callState)
        manager.onMessageReceived(
            SignalingMessage.CallAcceptMessage(
                callId = calling.callId,
                from = "device-01",
                to = "operator-test-01"
            )
        )

        val inCall = assertIs<CallState.InCall>(manager.state.value.callState)
        assertFalse(inCall.isMicrophoneMuted)
        assertFalse(fakeMedia.isMicrophoneMuted)

        // Toggle mute -> true
        manager.toggleMicrophoneMute()
        val mutedCall = assertIs<CallState.InCall>(manager.state.value.callState)
        assertTrue(mutedCall.isMicrophoneMuted)
        assertTrue(fakeMedia.isMicrophoneMuted)

        // Toggle mute -> false
        manager.toggleMicrophoneMute()
        val unmutedCall = assertIs<CallState.InCall>(manager.state.value.callState)
        assertFalse(unmutedCall.isMicrophoneMuted)
        assertFalse(fakeMedia.isMicrophoneMuted)
    }

    @Test
    fun testCallTeardownOnHangupRejectOfflineAndDisconnect() {
        // Case 1: Local hangup
        val media1 = FakeDesktopMediaController()
        val manager1 = createRegisteredManager(media1)
        manager1.invite("device-01")
        val callId1 = (manager1.state.value.callState as CallState.Calling).callId
        manager1.onMessageReceived(SignalingMessage.CallAcceptMessage(callId = callId1, from = "device-01"))
        assertTrue(media1.isCallActive)
        manager1.hangup()
        assertFalse(media1.isCallActive)
        assertEquals(CallState.Idle, manager1.state.value.callState)

        // Case 2: Remote reject
        val media2 = FakeDesktopMediaController()
        val manager2 = createRegisteredManager(media2)
        manager2.invite("device-01")
        val callId2 = (manager2.state.value.callState as CallState.Calling).callId
        manager2.onMessageReceived(SignalingMessage.CallRejectMessage(callId = callId2, from = "device-01"))
        assertFalse(media2.isCallActive)
        assertEquals(CallState.Idle, manager2.state.value.callState)

        // Case 3: Remote offline during call
        val media3 = FakeDesktopMediaController()
        val manager3 = createRegisteredManager(media3)
        manager3.invite("device-01")
        val callId3 = (manager3.state.value.callState as CallState.Calling).callId
        manager3.onMessageReceived(SignalingMessage.CallAcceptMessage(callId = callId3, from = "device-01"))
        assertTrue(media3.isCallActive)
        manager3.onMessageReceived(SignalingMessage.PeerOfflineMessage(peerId = "device-01"))
        assertFalse(media3.isCallActive)
        assertEquals(CallState.Idle, manager3.state.value.callState)

        // Case 4: Disconnect
        val media4 = FakeDesktopMediaController()
        val manager4 = createRegisteredManager(media4)
        manager4.invite("device-01")
        val callId4 = (manager4.state.value.callState as CallState.Calling).callId
        manager4.onMessageReceived(SignalingMessage.CallAcceptMessage(callId = callId4, from = "device-01"))
        assertTrue(media4.isCallActive)
        manager4.disconnect()
        assertFalse(media4.isCallActive)
    }

    @Test
    fun testMediaErrorTriggersHangup() {
        val fakeMedia = FakeDesktopMediaController()
        val manager = createRegisteredManager(fakeMedia)

        manager.invite("device-01")
        val callId = (manager.state.value.callState as CallState.Calling).callId
        manager.onMessageReceived(
            SignalingMessage.CallAcceptMessage(
                callId = callId,
                from = "device-01",
                to = "operator-test-01"
            )
        )
        assertTrue(fakeMedia.isCallActive)

        // Emit error from media layer
        fakeMedia.emitError("카메라 접근 불가")

        // Should be torn down
        assertFalse(fakeMedia.isCallActive)
        assertEquals(CallState.Idle, manager.state.value.callState)
        assertTrue(manager.state.value.logs.any { it.payload.contains("카메라 접근 불가") })
    }
}
