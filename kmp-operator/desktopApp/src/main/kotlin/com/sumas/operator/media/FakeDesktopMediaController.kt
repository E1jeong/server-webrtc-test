package com.sumas.operator.media

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class FakeDesktopMediaController(
    var autoGenerateOffer: Boolean = true,
    var autoGenerateIce: Boolean = true,
    var mockOfferSdp: String = "v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\n",
    var mockIceCandidate: String = "candidate:1 1 UDP 2130706431 127.0.0.1 50000 typ host"
) : DesktopMediaController {

    private val _remoteVideoFrame = MutableStateFlow<ImageBitmap?>(null)
    override val remoteVideoFrame: StateFlow<ImageBitmap?> = _remoteVideoFrame.asStateFlow()

    private var listener: DesktopMediaListener? = null

    var activeCallId: String? = null
        private set
    var activeTargetDeviceId: String? = null
        private set
    var isCallActive: Boolean = false
        private set
    var isMicrophoneMuted: Boolean = false
        private set
    var isMediaReady: Boolean = false
        private set

    val receivedAnswers = CopyOnWriteArrayList<String>()
    val receivedCandidates = CopyOnWriteArrayList<IceCandidateData>()
    val pendingIceCandidates = CopyOnWriteArrayList<IceCandidateData>()
    var remoteDescriptionSet: Boolean = false
        private set

    override fun setListener(listener: DesktopMediaListener?) {
        this.listener = listener
    }

    override fun startCall(callId: String, targetDeviceId: String) {
        stopCall()
        activeCallId = callId
        activeTargetDeviceId = targetDeviceId
        isCallActive = true
        isMediaReady = false
        remoteDescriptionSet = false

        listener?.onMediaStatusChanged("카메라·마이크 준비 중")
        isMediaReady = true
        listener?.onMediaReadyChanged(true)

        if (autoGenerateOffer) {
            listener?.onLocalOffer(callId, targetDeviceId, mockOfferSdp)
            listener?.onMediaStatusChanged("음성·영상 연결 중")
        }

        if (autoGenerateIce) {
            listener?.onLocalIceCandidate(callId, targetDeviceId, mockIceCandidate, "0", 0)
        }
    }

    override fun handleRemoteAnswer(callId: String, sdp: String) {
        if (!isCallActive || activeCallId != callId) return
        receivedAnswers.add(sdp)
        remoteDescriptionSet = true
        listener?.onMediaStatusChanged("단말 미디어 수신 중")

        // Flush pending ICE candidates that arrived before Answer
        val pending = pendingIceCandidates.toList()
        pendingIceCandidates.clear()
        for (candidate in pending) {
            receivedCandidates.add(candidate)
        }
    }

    override fun handleRemoteIceCandidate(
        callId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    ) {
        if (!isCallActive || activeCallId != callId) return
        val iceData = IceCandidateData(candidate, sdpMid, sdpMLineIndex)
        if (!remoteDescriptionSet) {
            pendingIceCandidates.add(iceData)
        } else {
            receivedCandidates.add(iceData)
        }
    }

    override fun setMicrophoneMuted(isMuted: Boolean) {
        isMicrophoneMuted = isMuted
    }

    override fun stopCall() {
        activeCallId = null
        activeTargetDeviceId = null
        isCallActive = false
        isMediaReady = false
        isMicrophoneMuted = false
        remoteDescriptionSet = false
        pendingIceCandidates.clear()
        listener?.onMediaReadyChanged(false)
        listener?.onMediaStatusChanged("통화 대기")
    }

    // Helper functions for testing
    fun emitLocalOffer(callId: String, targetDeviceId: String, sdp: String) {
        listener?.onLocalOffer(callId, targetDeviceId, sdp)
    }

    fun emitLocalIce(callId: String, targetDeviceId: String, candidate: String, sdpMid: String? = "0", sdpMLineIndex: Int? = 0) {
        listener?.onLocalIceCandidate(callId, targetDeviceId, candidate, sdpMid, sdpMLineIndex)
    }

    fun emitError(error: String) {
        listener?.onError(error)
    }

    fun emitStatus(status: String) {
        listener?.onMediaStatusChanged(status)
    }

    fun setMockVideoFrame(frame: ImageBitmap?) {
        _remoteVideoFrame.value = frame
    }
}
