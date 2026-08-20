package com.sumas.operator.media.webrtc

import dev.onvoid.webrtc.PeerConnectionFactory
import dev.onvoid.webrtc.media.MediaDevices
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebrtcNativeLoadTest {

    @Test
    fun testPeerConnectionFactoryCreationAndNativeLoad() {
        // PeerConnectionFactory instance creation triggers native DLL load
        val factory = PeerConnectionFactory()
        assertNotNull(factory)

        // Enumerate audio and video capture devices
        val audioDevices = MediaDevices.getAudioCaptureDevices()
        val videoDevices = MediaDevices.getVideoCaptureDevices()
        println("Discovered Audio Capture Devices: ${audioDevices.size}")
        audioDevices.forEach { println(" - Audio: ${it.name}") }
        println("Discovered Video Capture Devices: ${videoDevices.size}")
        videoDevices.forEach { println(" - Video: ${it.name}") }

        // Clean up factory
        factory.dispose()
        assertTrue(true)
    }
}
