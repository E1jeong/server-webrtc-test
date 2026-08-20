package com.sumas.operator

import com.sumas.operator.model.Peer
import com.sumas.operator.model.PeerType
import com.sumas.operator.signaling.SignalingJson
import com.sumas.operator.signaling.SignalingMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignalingJsonTest {

    @Test
    fun testRegisterMessageSerialization() {
        val message: SignalingMessage = SignalingMessage.RegisterMessage(
            peerId = "operator-01",
            peerType = PeerType.OPERATOR
        )
        val json = SignalingJson.encodeToString(SignalingMessage.serializer(), message)
        assertTrue(json.contains("\"type\":\"register\""))
        assertTrue(json.contains("\"peerId\":\"operator-01\""))
        assertTrue(json.contains("\"peerType\":\"operator\""))

        val decoded = SignalingJson.decodeFromString(SignalingMessage.serializer(), json)
        assertIs<SignalingMessage.RegisterMessage>(decoded)
        assertEquals("operator-01", decoded.peerId)
        assertEquals(PeerType.OPERATOR, decoded.peerType)
    }

    @Test
    fun testRegisteredMessageSerialization() {
        val jsonInput = """
            {
                "type": "registered",
                "peerId": "operator-01",
                "peers": [
                    {"peerId": "device-01", "peerType": "device"},
                    {"peerId": "operator-01", "peerType": "operator"}
                ]
            }
        """.trimIndent()

        val decoded = SignalingJson.decodeFromString(SignalingMessage.serializer(), jsonInput)
        assertIs<SignalingMessage.RegisteredMessage>(decoded)
        assertEquals("operator-01", decoded.peerId)
        assertEquals(2, decoded.peers.size)
        assertEquals(Peer("device-01", PeerType.DEVICE), decoded.peers[0])
    }

    @Test
    fun testPeerListMessageSerialization() {
        val request: SignalingMessage = SignalingMessage.PeerListMessage()
        val requestJson = SignalingJson.encodeToString(SignalingMessage.serializer(), request)
        assertTrue(requestJson.contains("\"type\":\"peer.list\""))

        val responseJson = """
            {
                "type": "peer.list",
                "peers": [{"peerId": "device-1", "peerType": "device"}]
            }
        """.trimIndent()
        val decoded = SignalingJson.decodeFromString(SignalingMessage.serializer(), responseJson)
        assertIs<SignalingMessage.PeerListMessage>(decoded)
        assertEquals(1, decoded.peers.size)
        assertEquals("device-1", decoded.peers[0].peerId)
    }

    @Test
    fun testPeerOnlineAndOfflineMessages() {
        val onlineJson = """{"type": "peer.online", "peerId": "device-2", "peerType": "device"}"""
        val decodedOnline = SignalingJson.decodeFromString(SignalingMessage.serializer(), onlineJson)
        assertIs<SignalingMessage.PeerOnlineMessage>(decodedOnline)
        assertEquals("device-2", decodedOnline.peerId)
        assertEquals(PeerType.DEVICE, decodedOnline.peerType)

        val offlineJson = """{"type": "peer.offline", "peerId": "device-2"}"""
        val decodedOffline = SignalingJson.decodeFromString(SignalingMessage.serializer(), offlineJson)
        assertIs<SignalingMessage.PeerOfflineMessage>(decodedOffline)
        assertEquals("device-2", decodedOffline.peerId)
    }

    @Test
    fun testCallRelayMessagesRoundTrip() {
        val invite: SignalingMessage = SignalingMessage.CallInviteMessage(
            callId = "call-1",
            to = "device-1"
        )
        val inviteJson = SignalingJson.encodeToString(SignalingMessage.serializer(), invite)
        val decodedInvite = SignalingJson.decodeFromString(SignalingMessage.serializer(), inviteJson)
        assertIs<SignalingMessage.CallInviteMessage>(decodedInvite)
        assertEquals("call-1", decodedInvite.callId)
        assertEquals("device-1", decodedInvite.to)

        val relayedAcceptJson = """
            {
                "type": "call.accept",
                "callId": "call-1",
                "from": "device-1",
                "to": "operator-1",
                "serverTimestamp": "2026-08-20T10:00:00.000Z"
            }
        """.trimIndent()
        val decodedAccept = SignalingJson.decodeFromString(SignalingMessage.serializer(), relayedAcceptJson)
        assertIs<SignalingMessage.CallAcceptMessage>(decodedAccept)
        assertEquals("call-1", decodedAccept.callId)
        assertEquals("device-1", decodedAccept.from)
        assertEquals("operator-1", decodedAccept.to)
        assertEquals("2026-08-20T10:00:00.000Z", decodedAccept.serverTimestamp)

        val reject: SignalingMessage = SignalingMessage.CallRejectMessage(callId = "call-1", to = "operator-1")
        val rejectJson = SignalingJson.encodeToString(SignalingMessage.serializer(), reject)
        val decodedReject = SignalingJson.decodeFromString(SignalingMessage.serializer(), rejectJson)
        assertIs<SignalingMessage.CallRejectMessage>(decodedReject)

        val hangup: SignalingMessage = SignalingMessage.CallHangupMessage(callId = "call-1", to = "device-1")
        val hangupJson = SignalingJson.encodeToString(SignalingMessage.serializer(), hangup)
        val decodedHangup = SignalingJson.decodeFromString(SignalingMessage.serializer(), hangupJson)
        assertIs<SignalingMessage.CallHangupMessage>(decodedHangup)
    }

    @Test
    fun testWebRtcRelayMessages() {
        val sdpContent = "v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
        val offer: SignalingMessage = SignalingMessage.WebRtcOfferMessage(
            callId = "call-1",
            sdp = sdpContent,
            to = "device-1"
        )
        val offerJson = SignalingJson.encodeToString(SignalingMessage.serializer(), offer)
        val decodedOffer = SignalingJson.decodeFromString(SignalingMessage.serializer(), offerJson)
        assertIs<SignalingMessage.WebRtcOfferMessage>(decodedOffer)
        assertEquals(sdpContent, decodedOffer.sdp)

        val answer: SignalingMessage = SignalingMessage.WebRtcAnswerMessage(
            callId = "call-1",
            sdp = sdpContent,
            from = "device-1",
            to = "operator-1",
            serverTimestamp = "2026-08-20T10:00:01.000Z"
        )
        val answerJson = SignalingJson.encodeToString(SignalingMessage.serializer(), answer)
        val decodedAnswer = SignalingJson.decodeFromString(SignalingMessage.serializer(), answerJson)
        assertIs<SignalingMessage.WebRtcAnswerMessage>(decodedAnswer)
        assertEquals(sdpContent, decodedAnswer.sdp)
        assertEquals("device-1", decodedAnswer.from)
        assertEquals("operator-1", decodedAnswer.to)
        assertEquals("2026-08-20T10:00:01.000Z", decodedAnswer.serverTimestamp)

        val iceJson = """
            {
                "type": "webrtc.ice",
                "callId": "call-1",
                "candidate": "candidate:1 1 UDP 2122260223 192.168.0.100 54321 typ host",
                "sdpMid": "0",
                "sdpMLineIndex": 0,
                "from": "device-1",
                "to": "operator-1"
            }
        """.trimIndent()
        val decodedIce = SignalingJson.decodeFromString(SignalingMessage.serializer(), iceJson)
        assertIs<SignalingMessage.WebRtcIceMessage>(decodedIce)
        assertEquals("0", decodedIce.sdpMid)
        assertEquals(0, decodedIce.sdpMLineIndex)
        assertTrue(decodedIce.candidate.contains("candidate:1"))
    }

    @Test
    fun testErrorMessageAndUnknownFields() {
        val errorJson = """
            {
                "type": "error",
                "code": "peer_offline",
                "message": "Target peer is not connected: device-1",
                "unknownField": 12345
            }
        """.trimIndent()
        val decoded = SignalingJson.decodeFromString(SignalingMessage.serializer(), errorJson)
        assertIs<SignalingMessage.ErrorMessage>(decoded)
        assertEquals("peer_offline", decoded.code)
        assertEquals("Target peer is not connected: device-1", decoded.message)
    }
}
