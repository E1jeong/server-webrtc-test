package com.sumas.operator.media.webrtc

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import dev.onvoid.webrtc.media.video.I420Buffer
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

class I420ToImageBitmapConverter {

    private var currentWidth: Int = 0
    private var currentHeight: Int = 0
    private var cachedBufferedImage: BufferedImage? = null
    private var pixelBuffer: IntArray? = null

    private var rotatedWidth: Int = 0
    private var rotatedHeight: Int = 0
    private var cachedRotatedImage: BufferedImage? = null

    @Synchronized
    fun convert(i420: I420Buffer, rotation: Int = 0): ImageBitmap {
        val width = i420.width
        val height = i420.height

        val normalizedRotation = ((rotation % 360) + 360) % 360

        if (cachedBufferedImage == null || currentWidth != width || currentHeight != height) {
            currentWidth = width
            currentHeight = height
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            cachedBufferedImage = image
            val raster = image.raster
            val dataBuffer = raster.dataBuffer as DataBufferInt
            pixelBuffer = dataBuffer.data
        }

        val outPixels = pixelBuffer ?: IntArray(width * height).also { pixelBuffer = it }

        val yBuffer = i420.dataY
        val uBuffer = i420.dataU
        val vBuffer = i420.dataV

        val strideY = i420.strideY
        val strideU = i420.strideU
        val strideV = i420.strideV

        var pixelIndex = 0

        for (row in 0 until height) {
            val yRowOffset = row * strideY
            val uvRowOffset = (row shr 1) * strideU
            val vRowOffset = (row shr 1) * strideV

            for (col in 0 until width) {
                val y = yBuffer.get(yRowOffset + col).toInt() and 0xFF
                val uvColOffset = col shr 1
                val u = (uBuffer.get(uvRowOffset + uvColOffset).toInt() and 0xFF) - 128
                val v = (vBuffer.get(vRowOffset + uvColOffset).toInt() and 0xFF) - 128

                val r = (y + (1.370705f * v)).toInt().coerceIn(0, 255)
                val g = (y - (0.337633f * u) - (0.698001f * v)).toInt().coerceIn(0, 255)
                val b = (y + (1.732446f * u)).toInt().coerceIn(0, 255)

                outPixels[pixelIndex++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val rawImage = cachedBufferedImage ?: BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        if (normalizedRotation == 0) {
            return rawImage.toComposeImageBitmap()
        }

        val targetWidth = if (normalizedRotation == 90 || normalizedRotation == 270) height else width
        val targetHeight = if (normalizedRotation == 90 || normalizedRotation == 270) width else height

        if (cachedRotatedImage == null || rotatedWidth != targetWidth || rotatedHeight != targetHeight) {
            rotatedWidth = targetWidth
            rotatedHeight = targetHeight
            cachedRotatedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        }

        val rotImage = cachedRotatedImage ?: BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = rotImage.createGraphics()
        try {
            when (normalizedRotation) {
                90 -> {
                    g2d.translate(targetWidth.toDouble(), 0.0)
                    g2d.rotate(Math.toRadians(90.0))
                }
                180 -> {
                    g2d.translate(targetWidth.toDouble(), targetHeight.toDouble())
                    g2d.rotate(Math.toRadians(180.0))
                }
                270 -> {
                    g2d.translate(0.0, targetHeight.toDouble())
                    g2d.rotate(Math.toRadians(270.0))
                }
            }
            g2d.drawImage(rawImage, 0, 0, null)
        } finally {
            g2d.dispose()
        }

        return rotImage.toComposeImageBitmap()
    }
}
