package com.sumas.operator.media.webrtc

import com.sumas.operator.media.DesktopMediaController
import com.sumas.operator.media.DesktopMediaListener
import dev.onvoid.webrtc.CreateSessionDescriptionObserver
import dev.onvoid.webrtc.PeerConnectionFactory
import dev.onvoid.webrtc.PeerConnectionObserver
import dev.onvoid.webrtc.RTCConfiguration
import dev.onvoid.webrtc.RTCIceCandidate
import dev.onvoid.webrtc.RTCOfferOptions
import dev.onvoid.webrtc.RTCPeerConnection
import dev.onvoid.webrtc.RTCPeerConnectionState
import dev.onvoid.webrtc.RTCRtpTransceiver
import dev.onvoid.webrtc.RTCRtpTransceiverDirection
import dev.onvoid.webrtc.RTCRtpTransceiverInit
import dev.onvoid.webrtc.RTCSdpType
import dev.onvoid.webrtc.RTCSessionDescription
import dev.onvoid.webrtc.SetSessionDescriptionObserver
import dev.onvoid.webrtc.media.MediaDevices
import dev.onvoid.webrtc.media.MediaType
import dev.onvoid.webrtc.media.audio.AudioOptions
import dev.onvoid.webrtc.media.audio.AudioTrack
import dev.onvoid.webrtc.media.audio.AudioTrackSource
import dev.onvoid.webrtc.media.video.VideoDeviceSource
import dev.onvoid.webrtc.media.video.VideoTrack
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow
import java.util.Collections

class WebrtcDesktopMediaController : DesktopMediaController {

    private var listener: DesktopMediaListener? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: RTCPeerConnection? = null

    private var audioSource: AudioTrackSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoDeviceSource: VideoDeviceSource? = null
    private var videoTrack: VideoTrack? = null

    val remoteVideoSink = WebrtcVideoRendererSink()
    override val remoteVideoFrame: StateFlow<ImageBitmap?> get() = remoteVideoSink.videoFrame

    private var activeCallId: String? = null
    private var activeTargetDeviceId: String? = null

    override fun setListener(listener: DesktopMediaListener?) {
        this.listener = listener
    }

    @Synchronized
    override fun startCall(callId: String, targetDeviceId: String) {
        stopCall()
        this.activeCallId = callId
        this.activeTargetDeviceId = targetDeviceId

        try {
            listener?.onMediaStatusChanged("장비 및 WebRTC 초기화 중")

            val pcf = factory ?: PeerConnectionFactory().also { factory = it }

            // 1. Audio setup
            val audioOptions = AudioOptions().apply {
                echoCancellation = true
                autoGainControl = true
                noiseSuppression = true
            }
            val aSource = pcf.createAudioSource(audioOptions)
            audioSource = aSource
            val aTrack = pcf.createAudioTrack("local_audio_$callId", aSource)
            audioTrack = aTrack

            // 2. Video setup (if camera available)
            val videoDevices = MediaDevices.getVideoCaptureDevices()
            if (videoDevices.isNotEmpty()) {
                val vSource = VideoDeviceSource()
                vSource.setVideoCaptureDevice(videoDevices[0])
                vSource.start()
                videoDeviceSource = vSource
                val vTrack = pcf.createVideoTrack("local_video_$callId", vSource)
                videoTrack = vTrack
            }

            // 3. PeerConnection setup
            val config = RTCConfiguration()
            val observer = object : PeerConnectionObserver {
                override fun onIceCandidate(candidate: RTCIceCandidate) {
                    listener?.onLocalIceCandidate(
                        callId = callId,
                        targetDeviceId = targetDeviceId,
                        candidate = candidate.sdp,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex
                    )
                }

                override fun onTrack(transceiver: RTCRtpTransceiver) {
                    val track = transceiver.receiver?.track
                    if (track is VideoTrack) {
                        remoteVideoSink.attachTrack(track)
                        listener?.onMediaStatusChanged("단말 영상 수신 중")
                    }
                }

                override fun onConnectionChange(state: RTCPeerConnectionState) {
                    listener?.onMediaStatusChanged("WebRTC $state")
                    if (state == RTCPeerConnectionState.CONNECTED) {
                        listener?.onMediaReadyChanged(true)
                    } else if (state == RTCPeerConnectionState.DISCONNECTED ||
                        state == RTCPeerConnectionState.FAILED ||
                        state == RTCPeerConnectionState.CLOSED
                    ) {
                        listener?.onMediaReadyChanged(false)
                    }
                }
            }

            val pc = pcf.createPeerConnection(config, observer)
            peerConnection = pc

            // 4. Add tracks
            aTrack.let { pc.addTrack(it, Collections.singletonList("stream0")) }
            videoTrack?.let { pc.addTrack(it, Collections.singletonList("stream0")) }

            // 5. Create SDP Offer
            val offerOptions = RTCOfferOptions()
            pc.createOffer(offerOptions, object : CreateSessionDescriptionObserver {
                override fun onSuccess(description: RTCSessionDescription) {
                    pc.setLocalDescription(description, object : SetSessionDescriptionObserver {
                        override fun onSuccess() {
                            listener?.onLocalOffer(callId, targetDeviceId, description.sdp)
                            listener?.onMediaStatusChanged("음성·영상 연결 중")
                        }

                        override fun onFailure(error: String?) {
                            listener?.onError("LocalDescription 설정 실패: $error")
                        }
                    })
                }

                override fun onFailure(error: String?) {
                    listener?.onError("Offer 생성 실패: $error")
                }
            })

        } catch (e: Exception) {
            listener?.onError("통화 미디어 시작 실패: ${e.message}")
            stopCall()
        }
    }

    @Synchronized
    override fun handleRemoteAnswer(callId: String, sdp: String) {
        val pc = peerConnection ?: return
        if (activeCallId != callId) return

        val sessionDescription = RTCSessionDescription(RTCSdpType.ANSWER, sdp)
        pc.setRemoteDescription(sessionDescription, object : SetSessionDescriptionObserver {
            override fun onSuccess() {
                listener?.onMediaStatusChanged("원격 Answer 적용됨")
            }

            override fun onFailure(error: String?) {
                listener?.onError("RemoteDescription 적용 실패: $error")
            }
        })
    }

    @Synchronized
    override fun handleRemoteIceCandidate(
        callId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    ) {
        val pc = peerConnection ?: return
        if (activeCallId != callId) return

        val iceCandidate = RTCIceCandidate(
            sdpMid ?: "0",
            sdpMLineIndex ?: 0,
            candidate
        )
        try {
            pc.addIceCandidate(iceCandidate)
        } catch (e: Exception) {
            listener?.onError("ICE 후보 추가 실패: ${e.message}")
        }
    }

    @Synchronized
    override fun setMicrophoneMuted(isMuted: Boolean) {
        audioTrack?.let {
            it.isEnabled = !isMuted
            listener?.onMediaStatusChanged(if (isMuted) "마이크 음소거됨" else "마이크 켜짐")
        }
    }

    @Synchronized
    override fun stopCall() {
        remoteVideoSink.detachTrack()

        videoTrack = null
        audioTrack = null
        audioSource = null

        try {
            videoDeviceSource?.stop()
            videoDeviceSource?.dispose()
        } catch (_: Throwable) {}
        videoDeviceSource = null

        try {
            peerConnection?.close()
        } catch (_: Throwable) {}
        peerConnection = null

        activeCallId = null
        activeTargetDeviceId = null

        listener?.onMediaReadyChanged(false)
        listener?.onMediaStatusChanged("통화 종료됨")
    }

    @Synchronized
    fun release() {
        stopCall()
        try {
            factory?.dispose()
        } catch (_: Throwable) {}
        factory = null
    }
}
