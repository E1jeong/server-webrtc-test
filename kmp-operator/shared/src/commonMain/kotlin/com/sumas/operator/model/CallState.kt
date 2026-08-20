package com.sumas.operator.model

sealed interface CallState {
    data object Idle : CallState

    data class Calling(
        val targetPeerId: String,
        val callId: String,
        val startedAt: Long = 0L
    ) : CallState

    data class InCall(
        val peerId: String,
        val callId: String,
        val statusText: String = "통화 중",
        val isMicrophoneMuted: Boolean = false,
        val isMediaReady: Boolean = false
    ) : CallState
}
