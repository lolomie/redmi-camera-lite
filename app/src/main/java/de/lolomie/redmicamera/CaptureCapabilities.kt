package de.lolomie.redmicamera

/** Immutable API evidence, independent of Android so policy can be tested on the JVM. */
data class CaptureSize(val width: Int, val height: Int) {
    init { require(width > 0 && height > 0) }
    val pixels: Long get() = width.toLong() * height
    val megapixels: Double get() = pixels / 1_000_000.0
}

data class CaptureFpsRange(val lower: Int, val upper: Int) {
    init { require(lower > 0 && upper >= lower) }
    operator fun contains(fps: Int): Boolean = fps in lower..upper
}

data class VideoStreamEvidence(val size: CaptureSize, val minFrameDurationNs: Long)
data class HighSpeedStreamEvidence(val size: CaptureSize, val fpsRanges: List<CaptureFpsRange>)

data class CaptureCapabilities(
    val cameraId: String,
    val jpegSizes: List<CaptureSize> = emptyList(),
    val ultraHighResolutionSensor: Boolean = false,
    val maximumResolutionJpegSizes: List<CaptureSize> = emptyList(),
    val maximumResolutionSensorSize: CaptureSize? = null,
    val sensorPixelModeRequestAvailable: Boolean = false,
    val regularAeFpsRanges: List<CaptureFpsRange> = emptyList(),
    val regularVideoStreams: List<VideoStreamEvidence> = emptyList(),
    val highSpeedStreams: List<HighSpeedStreamEvidence> = emptyList(),
)

/** API exposure permits a feasibility investigation, never a DEVICE_VERIFIED UI claim. */
data class ModeExposure(val apiExposed: Boolean, val reason: String) {
    val deviceVerified: Boolean get() = false
}

object CaptureModePolicy {
    // Marketing "200 MP" can represent e.g. 16320x12240 = 199,756,800 native pixels.
    private const val NATIVE_200_MP_MIN_PIXELS = 190_000_000L

    fun regularVideo(
        camera: CaptureCapabilities,
        size: CaptureSize,
        fps: Int,
        encoderSupports: Boolean,
    ): ModeExposure {
        if (fps <= 0) return blocked("Invalid frame rate")
        if (camera.regularAeFpsRanges.none { fps in it }) {
            return blocked("Regular-session AE does not expose ${fps}fps; high-speed ranges are separate")
        }
        val stream = camera.regularVideoStreams.firstOrNull { it.size == size }
            ?: return blocked("Requested size is not exposed for regular video")
        if (stream.minFrameDurationNs <= 0) return blocked("Regular video frame timing is unknown")
        // Camera metadata rounds duration to integer nanoseconds; allow at most one ns rounding.
        if (stream.minFrameDurationNs > 1_000_000_000L / fps + 1) {
            return blocked("Regular stream minimum frame duration cannot sustain ${fps}fps")
        }
        if (!encoderSupports) return blocked("No encoder exposes this exact size and frame rate")
        return exposed("Regular AE, stream timing and encoder expose the combination; capture validation pending")
    }

    fun highSpeedVideo(
        camera: CaptureCapabilities,
        size: CaptureSize,
        fps: Int,
        encoderSupports: Boolean,
    ): ModeExposure {
        if (fps < 120) return blocked("High-speed evidence cannot enable ordinary 60fps recording")
        val fixedRateExposed = camera.highSpeedStreams.any { stream ->
            stream.size == size && stream.fpsRanges.any { it.lower == fps && it.upper == fps }
        }
        if (!fixedRateExposed) return blocked("Exact fixed-rate constrained-high-speed size/FPS pair is not exposed")
        if (!encoderSupports) return blocked("No encoder exposes this exact high-speed size and frame rate")
        return exposed("Constrained-high-speed pair and encoder exposed; dedicated session and playback validation pending")
    }

    fun ultraHighResolutionPhoto(camera: CaptureCapabilities): ModeExposure {
        if (!camera.ultraHighResolutionSensor) return blocked("UHR sensor capability is not exposed")
        if (!camera.sensorPixelModeRequestAvailable) return blocked("SENSOR_PIXEL_MODE request is not exposed")
        val nativeSize = camera.maximumResolutionJpegSizes.maxByOrNull { it.pixels }
            ?: return blocked("Maximum-resolution JPEG stream map is not exposed")
        if (nativeSize.pixels < NATIVE_200_MP_MIN_PIXELS) return blocked("No native approximately 200MP JPEG output exposed")
        val sensor = camera.maximumResolutionSensorSize
            ?: return blocked("Maximum-resolution sensor pixel array is not exposed")
        if (sensor.width < nativeSize.width || sensor.height < nativeSize.height) {
            return blocked("JPEG dimensions exceed exposed maximum-resolution sensor dimensions")
        }
        return exposed("Native approximately 200MP UHR JPEG API path exposed; capture and native dimensions validation pending")
    }

    private fun blocked(reason: String) = ModeExposure(false, reason)
    private fun exposed(reason: String) = ModeExposure(true, reason)
}
