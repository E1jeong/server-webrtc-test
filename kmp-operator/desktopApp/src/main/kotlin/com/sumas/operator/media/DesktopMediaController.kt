package com.sumas.operator.media

interface DesktopMediaController {
    fun setListener(listener: DesktopMediaListener?)
    fun startCall(callId: String, targetDeviceId: String)
    fun handleRemoteAnswer(callId: String, sdp: String)
    fun handleRemoteIceCandidate(
        callId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    )
    fun setMicrophoneMuted(isMuted: Boolean)
    fun stopCall()
}
