package com.sumas.operator

import androidx.compose.ui.graphics.ImageBitmap
import com.sumas.operator.media.FakeDesktopMediaController
import com.sumas.operator.media.webrtc.I420ToImageBitmapConverter
import com.sumas.operator.media.webrtc.WebrtcDesktopMediaController
import com.sumas.operator.model.CallState
import com.sumas.operator.model.ConnectionStatus
import com.sumas.operator.model.Peer
import com.sumas.operator.model.PeerType
import com.sumas.operator.signaling.SignalingMessage
import com.sumas.operator.state.DesktopOperatorManager
import dev.onvoid.webrtc.media.video.I420Buffer
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopMediaUiIntegrationTest {

    @Test
    fun testMockVideoFrameUpdatesFlowThroughManager() {
        val fakeMedia = FakeDesktopMediaController()
        val manager = DesktopOperatorManager(mediaController = fakeMedia)

        // Initially null
        assertNull(manager.remoteVideoFrame.value)

        // Mock bitmap frame
        val mockBitmap = ImageBitmap(640, 480)
        fakeMedia.setMockVideoFrame(mockBitmap)

        assertEquals(mockBitmap, manager.remoteVideoFrame.value)
        assertEquals(640, manager.remoteVideoFrame.value?.width)
        assertEquals(480, manager.remoteVideoFrame.value?.height)

        // Clear frame
        fakeMedia.setMockVideoFrame(null)
        assertNull(manager.remoteVideoFrame.value)
    }

    @Test
    fun testI420ConverterProducesValidComposeImageBitmap() {
        val converter = I420ToImageBitmapConverter()
        val width = 160
        val height = 120

        val yBuffer = ByteBuffer.allocateDirect(width * height)
        val uBuffer = ByteBuffer.allocateDirect((width / 2) * (height / 2))
        val vBuffer = ByteBuffer.allocateDirect((width / 2) * (height / 2))

        for (i in 0 until (width * height)) {
            yBuffer.put(i, (i % 255).toByte())
        }
        for (i in 0 until ((width / 2) * (height / 2))) {
            uBuffer.put(i, 128.toByte())
            vBuffer.put(i, 128.toByte())
        }

        val i420Buffer = object : I420Buffer {
            override fun getWidth(): Int = width
            override fun getHeight(): Int = height
            override fun getDataY(): ByteBuffer = yBuffer
            override fun getDataU(): ByteBuffer = uBuffer
            override fun getDataV(): ByteBuffer = vBuffer
            override fun getStrideY(): Int = width
            override fun getStrideU(): Int = width / 2
            override fun getStrideV(): Int = width / 2
            override fun retain() {}
            override fun release() {}
            override fun toI420(): I420Buffer = this
            override fun cropAndScale(
                cropX: Int,
                cropY: Int,
                cropWidth: Int,
                cropHeight: Int,
                scaleWidth: Int,
                scaleHeight: Int
            ): dev.onvoid.webrtc.media.video.VideoFrameBuffer = this
        }
        val bitmap = converter.convert(i420Buffer)

        assertNotNull(bitmap)
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
    }

    @Test
    fun testManagerWithWebrtcControllerInitializesAndCleansUp() {
        val controller = WebrtcDesktopMediaController()
        val manager = DesktopOperatorManager(mediaController = controller)

        assertEquals(ConnectionStatus.DISCONNECTED, manager.state.value.connectionStatus)
        assertEquals(CallState.Idle, manager.state.value.callState)
        assertNull(manager.remoteVideoFrame.value)

        manager.cleanup()
    }
}
