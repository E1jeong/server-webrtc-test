package com.sumas.operator.media.webrtc

import com.sumas.operator.media.DesktopMediaListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebrtcDesktopMediaControllerTest {

    private lateinit var controller: WebrtcDesktopMediaController

    @BeforeTest
    fun setup() {
        controller = WebrtcDesktopMediaController()
    }

    @AfterTest
    fun tearDown() {
        controller.release()
    }

    @Test
    fun testStartCallAndOfferGeneration() {
        val offerLatch = CountDownLatch(1)
        val generatedOffer = AtomicReference<String>()
        val receivedCallId = AtomicReference<String>()
        val receivedTargetId = AtomicReference<String>()

        val listener = object : DesktopMediaListener {
            override fun onLocalOffer(callId: String, targetDeviceId: String, sdp: String) {
                receivedCallId.set(callId)
                receivedTargetId.set(targetDeviceId)
                generatedOffer.set(sdp)
                offerLatch.countDown()
            }

            override fun onLocalIceCandidate(
                callId: String,
                targetDeviceId: String,
                candidate: String,
                sdpMid: String?,
                sdpMLineIndex: Int?
            ) {
            }

            override fun onMediaStatusChanged(statusText: String) {
                println("Status: $statusText")
            }

            override fun onMediaReadyChanged(isReady: Boolean) {
            }

            override fun onError(error: String) {
                println("Error: $error")
            }
        }

        controller.setListener(listener)
        controller.startCall("call-test-123", "device-test-456")

        val completed = offerLatch.await(5, TimeUnit.SECONDS)
        assertTrue(completed, "Local SDP Offer should be generated within 5 seconds")
        assertNotNull(generatedOffer.get(), "Generated SDP Offer must not be null")
        assertTrue(generatedOffer.get().contains("v=0"), "SDP must contain v=0")
        assertTrue(generatedOffer.get().contains("m=audio"), "SDP must contain m=audio")
        assertTrue(generatedOffer.get().contains("m=video"), "SDP must contain m=video")

        controller.stopCall()
    }

    @Test
    fun testRepeatedCallAndTeardownLifecycle() {
        for (i in 1..5) {
            val offerLatch = CountDownLatch(1)
            val listener = object : DesktopMediaListener {
                override fun onLocalOffer(callId: String, targetDeviceId: String, sdp: String) {
                    offerLatch.countDown()
                }

                override fun onLocalIceCandidate(
                    callId: String,
                    targetDeviceId: String,
                    candidate: String,
                    sdpMid: String?,
                    sdpMLineIndex: Int?
                ) {}

                override fun onMediaStatusChanged(statusText: String) {}
                override fun onMediaReadyChanged(isReady: Boolean) {}
                override fun onError(error: String) {
                    println("Iteration $i Error: $error")
                }
            }

            controller.setListener(listener)
            controller.startCall("call-rep-$i", "device-rep-$i")
            val success = offerLatch.await(3, TimeUnit.SECONDS)
            assertTrue(success, "Iteration $i failed to generate offer")
            controller.stopCall()
        }
    }

    @Test
    fun testMicrophoneMuteToggle() {
        controller.startCall("call-mute-test", "device-mute-test")
        controller.setMicrophoneMuted(true)
        controller.setMicrophoneMuted(false)
        controller.stopCall()
    }
}
