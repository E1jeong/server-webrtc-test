package com.sumas.operator.media

interface DesktopMediaListener {
    fun onLocalOffer(callId: String, targetDeviceId: String, sdp: String)
    fun onLocalIceCandidate(
        callId: String,
        targetDeviceId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    )
    fun onMediaStatusChanged(statusText: String)
    fun onMediaReadyChanged(isReady: Boolean)
    fun onError(error: String)
}
