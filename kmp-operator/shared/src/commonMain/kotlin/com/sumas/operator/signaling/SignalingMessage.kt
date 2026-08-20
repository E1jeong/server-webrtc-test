package com.sumas.operator.signaling

import com.sumas.operator.model.Peer
import com.sumas.operator.model.PeerType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface SignalingMessage {

    @Serializable
    @SerialName("register")
    data class RegisterMessage(
        val peerId: String,
        val peerType: PeerType
    ) : SignalingMessage

    @Serializable
    @SerialName("registered")
    data class RegisteredMessage(
        val peerId: String,
        val peers: List<Peer> = emptyList()
    ) : SignalingMessage

    @Serializable
    @SerialName("peer.list")
    data class PeerListMessage(
        val peers: List<Peer> = emptyList()
    ) : SignalingMessage

    @Serializable
    @SerialName("peer.online")
    data class PeerOnlineMessage(
        val peerId: String,
        val peerType: PeerType
    ) : SignalingMessage

    @Serializable
    @SerialName("peer.offline")
    data class PeerOfflineMessage(
        val peerId: String
    ) : SignalingMessage

    @Serializable
    @SerialName("call.invite")
    data class CallInviteMessage(
        val callId: String,
        val to: String? = null,
        val from: String? = null,
        val serverTimestamp: String? = null
    ) : SignalingMessage

    @Serializable
    @SerialName("call.accept")
    data class CallAcceptMessage(
        val callId: String,
        val to: String? = null,
        val from: String? = null,
        val serverTimestamp: String? = null
    ) : SignalingMessage

    @Serializable
    @SerialName("call.reject")
    data class CallRejectMessage(
        val callId: String,
        val to: String? = null,
        val from: String? = null,
        val serverTimestamp: String? = null
    ) : SignalingMessage

    @Serializable
    @SerialName("call.hangup")
    data class CallHangupMessage(
        val callId: String,
        val to: String? = null,
        val from: String? = null,
        val serverTimestamp: String? = null
    ) : SignalingMessage

    @Serializable
    @SerialName("webrtc.offer")
    data class WebRtcOfferMessage(
        val callId: String,
        val sdp: String,
        val to: String? = null,
        val from: String? = null,
        val serverTimestamp: String? = null
    ) : SignalingMessage

    @Serializable
    @SerialName("webrtc.answer")
    data class WebRtcAnswerMessage(
        val callId: String,
        val sdp: String,
        val to: String? = null,
        val from: String? = null,
        val serverTimestamp: String? = null
    ) : SignalingMessage

    @Serializable
    @SerialName("webrtc.ice")
    data class WebRtcIceMessage(
        val callId: String,
        val candidate: String,
        val sdpMid: String? = "0",
        val sdpMLineIndex: Int? = 0,
        val to: String? = null,
        val from: String? = null,
        val serverTimestamp: String? = null
    ) : SignalingMessage

    @Serializable
    @SerialName("error")
    data class ErrorMessage(
        val code: String,
        val message: String
    ) : SignalingMessage
}
