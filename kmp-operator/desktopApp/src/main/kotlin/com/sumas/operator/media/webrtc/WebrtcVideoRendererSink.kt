package com.sumas.operator.media.webrtc

import androidx.compose.ui.graphics.ImageBitmap
import dev.onvoid.webrtc.media.video.VideoFrame
import dev.onvoid.webrtc.media.video.VideoTrack
import dev.onvoid.webrtc.media.video.VideoTrackSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebrtcVideoRendererSink : VideoTrackSink {

    private val converter = I420ToImageBitmapConverter()
    private val _videoFrame = MutableStateFlow<ImageBitmap?>(null)
    val videoFrame: StateFlow<ImageBitmap?> = _videoFrame.asStateFlow()

    private var boundTrack: VideoTrack? = null

    override fun onVideoFrame(frame: VideoFrame) {
        val buffer = frame.buffer ?: return
        val i420 = buffer.toI420()
        try {
            val bitmap = converter.convert(i420, frame.rotation)
            _videoFrame.value = bitmap
        } finally {
            i420.release()
        }
    }

    @Synchronized
    fun attachTrack(track: VideoTrack) {
        detachTrack()
        boundTrack = track
        track.addSink(this)
    }

    @Synchronized
    fun detachTrack() {
        boundTrack?.let {
            try {
                it.removeSink(this)
            } catch (_: Exception) {
            }
        }
        boundTrack = null
        _videoFrame.value = null
    }
}
