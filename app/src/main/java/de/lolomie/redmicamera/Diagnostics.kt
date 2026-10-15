package de.lolomie.redmicamera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaCodecList
import android.media.MediaRecorder
import android.os.Build
import android.util.Size
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.DynamicRange
import androidx.camera.video.Recorder
import java.util.concurrent.TimeUnit
import kotlin.math.floor

/** Read-only API inventory. Call on a worker thread; never opens a capture session. */
object Diagnostics {
    fun collect(context: Context): String = buildString {
        appendLine("Redmi Camera Lite — sanitized capability report")
        appendLine("Manufacturer: ${Build.MANUFACTURER}; model: ${Build.MODEL}; Android API: ${Build.VERSION.SDK_INT}")
        appendLine("Android release: ${Build.VERSION.RELEASE}; security patch: ${Build.VERSION.SECURITY_PATCH}; firmware build ID: ${Build.ID}")
        appendLine("HyperOS version must be confirmed in system settings; no hidden property or fingerprint access.")
        appendLine("No serial, fingerprint, location, account or persistent device identifier collected.")
        appendLine("Evidence labels:")
        appendLine("OEM_ADVERTISED = manufacturer claim, not independently established by this report.")
        appendLine("API_EXPOSED = reported by Android APIs; does not prove a working mode or image quality.")
        appendLine("CAPTURED = actual saved media inspected; none assessed by this inventory.")
        appendLine("DEVICE_VERIFIED = repeatable capture and playback verified on this device; none assessed here.")
        appendLine("BLOCKED = unavailable, permission denied, API failure, or device validation still required.")
        appendLine("Enumeration alone never enables or certifies a selectable capture mode.")
        appendLine()

        var has200Mp = false
        var hasUltraHighResolution = false
        var has60FpsEvidence = false
        var hasSlowMotionEvidence = false
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val ids = try {
            manager?.cameraIdList?.toList().orEmpty()
        } catch (e: Exception) {
            appendLine("Camera enumeration BLOCKED: ${failure(e)}")
            emptyList()
        }
        appendLine("Camera2 API_EXPOSED logical camera IDs: ${ids.joinToString()}")
        for (id in ids.take(16)) {
            appendLine("\nCamera ID $id")
            try {
                val characteristics = manager!!.getCameraCharacteristics(id)
                val physicalIds = characteristics.physicalCameraIds.sorted()
                appendLine("  physical IDs: ${physicalIds.joinToString().ifEmpty { "none exposed" }}")
                val result = camera(characteristics)
                append(result.text)
                has200Mp = has200Mp || result.has200Mp
                hasUltraHighResolution = hasUltraHighResolution || result.hasUhr
                has60FpsEvidence = has60FpsEvidence || result.has60Fps
                hasSlowMotionEvidence = hasSlowMotionEvidence || result.hasSlowMotion
                for (physicalId in physicalIds.take(8)) {
                    if (physicalId in ids) continue
                    appendLine("  Physical camera $physicalId (not independently selectable unless separately exposed):")
                    try {
                        val physical = camera(manager.getCameraCharacteristics(physicalId))
                        append(physical.text)
                        has200Mp = has200Mp || physical.has200Mp
                        hasUltraHighResolution = hasUltraHighResolution || physical.hasUhr
                        has60FpsEvidence = has60FpsEvidence || physical.has60Fps
                        hasSlowMotionEvidence = hasSlowMotionEvidence || physical.hasSlowMotion
                    } catch (e: Exception) {
                        appendLine("    BLOCKED: ${failure(e)}")
                    }
                }
            } catch (e: Exception) {
                appendLine("  BLOCKED: ${failure(e)}")
            }
        }
        if (ids.size > 16) appendLine("Camera list truncated at 16 entries.")

        appendLine("\nCameraX API_EXPOSED recording qualities (not device capture verification):")
        try {
            val provider = ProcessCameraProvider.getInstance(context.applicationContext).get(8, TimeUnit.SECONDS)
            for (info in provider.availableCameraInfos.take(16)) {
                try {
                    appendLine("  facing=${facing(info.lensFacing)}; qualities=${Recorder.getVideoCapabilities(info).getSupportedQualities(DynamicRange.SDR).joinToString()}")
                } catch (e: Exception) {
                    appendLine("  quality enumeration BLOCKED: ${failure(e)}")
                }
            }
        } catch (e: Exception) {
            appendLine("  BLOCKED: ${failure(e)}")
        }

        appendLine("\nMediaCodec API_EXPOSED video encoders:")
        try {
            val encoders = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder }
            for (codec in encoders.take(40)) {
                for (type in codec.supportedTypes.filter { it.startsWith("video/") }.take(8)) {
                    try {
                        val caps = codec.getCapabilitiesForType(type)
                        val video = caps.videoCapabilities ?: continue
                        appendLine("  ${codec.name}: $type; hardware=${codec.isHardwareAccelerated}; software=${codec.isSoftwareOnly}")
                        appendLine("    width=${video.supportedWidths}; height=${video.supportedHeights}; alignment=${video.widthAlignment}x${video.heightAlignment}")
                        appendLine("    bitrate=${video.bitrateRange}; fps=${video.supportedFrameRates}")
                        appendLine("    profile/levels=${caps.profileLevels.take(12).joinToString { "${it.profile}/${it.level}" }}")
                        for (size in listOf(Size(1280, 720), Size(1920, 1080), Size(3840, 2160))) {
                            val supported = video.isSizeSupported(size.width, size.height)
                            appendLine("    $size: supported=$supported${if (supported) "; fps=${video.getSupportedFrameRatesFor(size.width, size.height)}; 60fps=${video.areSizeAndRateSupported(size.width, size.height, 60.0)}" else ""}")
                        }
                        appendLine("    Codec limits do not prove a camera+encoder configuration can run concurrently.")
                    } catch (e: Exception) {
                        appendLine("  ${codec.name} $type BLOCKED: ${failure(e)}")
                    }
                }
            }
            if (encoders.size > 40) appendLine("  Encoder list truncated at 40 entries.")
        } catch (e: Exception) {
            appendLine("  Codec enumeration BLOCKED: ${failure(e)}")
        }

        appendLine("\nFeature findings:")
        appendLine("  200MP: ${if (has200Mp) "API_EXPOSED JPEG size >= 190 million pixels" else "BLOCKED: no JPEG output >= 190 million pixels found"}; exact dimensions above; CAPTURED/DEVICE_VERIFIED pending.")
        appendLine("  Ultra-high-resolution sensor capability: $hasUltraHighResolution; does not by itself prove 200MP JPEG capture.")
        appendLine("  Ultrawide: focal lengths and sensor geometry above are API evidence only. No automatic lens-role claim; OEM mapping and device capture required.")
        appendLine("  60fps: ${if (has60FpsEvidence) "API_EXPOSED timing or AE range evidence" else "BLOCKED: no >=60fps timing/AE evidence found"}; a matching size, capture session and encoded file still require validation.")
        appendLine("  Slow motion: ${if (hasSlowMotionEvidence) "API_EXPOSED constrained-high-speed combinations" else "BLOCKED: no constrained-high-speed combinations exposed"}; capture and slow-motion playback not verified.")
        appendLine("  OEM_ADVERTISED: no manufacturer specifications fetched or verified by this report.")
        appendLine("  CAPTURED: none. DEVICE_VERIFIED: none. No test capture or camera session was created.")
    }

    private data class CameraEvidence(
        val text: String,
        val has200Mp: Boolean,
        val hasUhr: Boolean,
        val has60Fps: Boolean,
        val hasSlowMotion: Boolean,
    )

    private fun camera(c: CameraCharacteristics): CameraEvidence {
        var has200Mp = false
        var has60Fps = false
        var hasSlowMotion = false
        val capabilities = c[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] ?: intArrayOf()
        val uhr = Build.VERSION.SDK_INT >= 31 && capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR)
        val report = buildString {
            appendLine("    API_EXPOSED facing=${facing(c[CameraCharacteristics.LENS_FACING])}; hardwareLevel=${c[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]}")
            appendLine("    capability constants=${capabilities.joinToString()}")
            appendLine("    focal lengths mm=${c[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]?.joinToString() ?: "unknown"}")
            appendLine("    sensor physical size mm=${c[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]}; pixel array=${c[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]}")
            appendLine("    flash=${c[CameraCharacteristics.FLASH_INFO_AVAILABLE]}; OIS modes=${c[CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION]?.joinToString()}; EIS modes=${c[CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES]?.joinToString()}")
            appendLine("    max digital zoom=${c[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]}")
            appendLine("    AF modes=${c[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES]?.joinToString()}; minimum focus distance=${c[CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE]}")
            appendLine("    AE modes=${c[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]?.joinToString()}; compensation=${c[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]}; step=${c[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]}")
            appendLine("    ISO range=${c[CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE]}; exposure time ns=${c[CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE]}")
            appendLine("    AWB modes=${c[CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES]?.joinToString()}; manual requests require advertised capabilities and session validation.")
            if (Build.VERSION.SDK_INT >= 30) appendLine("    zoom ratio=${c[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]}")
            val fpsRanges = c[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES].orEmpty()
            has60Fps = fpsRanges.any { it.upper >= 60 }
            appendLine("    AE FPS ranges=${fpsRanges.joinToString()}; these ranges are not size/session guarantees.")

            fun jpeg(map: StreamConfigurationMap?, label: String) {
                if (map == null) {
                    appendLine("    $label: BLOCKED/not exposed")
                    return
                }
                val normal = map.getOutputSizes(ImageFormat.JPEG).orEmpty().sortedByDescending { pixels(it) }
                val high = map.getHighResolutionOutputSizes(ImageFormat.JPEG).orEmpty().sortedByDescending { pixels(it) }
                has200Mp = has200Mp || (normal + high).any { pixels(it) >= 190_000_000L }
                appendLine("    $label JPEG normal: max=${normal.firstOrNull()}; ${normal.take(8).joinToString()} (${normal.size} sizes)")
                appendLine("    $label JPEG high-resolution: max=${high.firstOrNull()}; ${high.take(8).joinToString()} (${high.size} sizes)")
            }

            val map = c[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            jpeg(map, "Default pixel mode")
            if (Build.VERSION.SDK_INT >= 31) {
                appendLine("    UHR capability=$uhr; maximum-resolution pixel array=${c[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION]}")
                appendLine("    SENSOR_PIXEL_MODE request key exposed=${c.availableCaptureRequestKeys?.contains(CaptureRequest.SENSOR_PIXEL_MODE) == true}")
                jpeg(c[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION], "Maximum-resolution pixel mode")
            } else appendLine("    UHR maximum-resolution API: BLOCKED/requires API 31")

            if (map != null) {
                val videoSizes = map.getOutputSizes(MediaRecorder::class.java).orEmpty().sortedByDescending { pixels(it) }
                appendLine("    MediaRecorder video outputs (${videoSizes.size} total, largest 16 shown):")
                for (size in videoSizes.take(16)) {
                    try {
                        val duration = map.getOutputMinFrameDuration(MediaRecorder::class.java, size)
                        val maxFps = if (duration > 0) floor(1_000_000_000.0 / duration).toInt() else null
                        has60Fps = has60Fps || (duration in 1..16_666_667L)
                        appendLine("      $size: minFrameDurationNs=$duration; nominalMaxFps=${maxFps ?: "unknown"}")
                    } catch (e: Exception) {
                        appendLine("      $size timing BLOCKED: ${failure(e)}")
                    }
                }
                if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)) {
                    val sizes = map.highSpeedVideoSizes.orEmpty().sortedByDescending { pixels(it) }
                    for (size in sizes.take(16)) {
                        val ranges = map.getHighSpeedVideoFpsRangesFor(size).orEmpty()
                        hasSlowMotion = hasSlowMotion || ranges.any { it.upper >= 120 }
                        appendLine("    High-speed API_EXPOSED $size: ${ranges.joinToString()}")
                    }
                } else appendLine("    Constrained high-speed video: not exposed")
            }
        }
        return CameraEvidence(report, has200Mp, uhr, has60Fps, hasSlowMotion)
    }

    private fun pixels(size: Size): Long = size.width.toLong() * size.height

    private fun facing(value: Int?): String = when (value) {
        CameraCharacteristics.LENS_FACING_FRONT -> "front"
        CameraCharacteristics.LENS_FACING_BACK -> "back"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
        else -> "unknown"
    }

    // Exception messages may contain device-specific data: emit class names only.
    private fun failure(error: Exception): String {
        val cause = error.cause ?: error
        return if (cause is SecurityException) "SecurityException (camera permission/access required)" else cause.javaClass.simpleName
    }
}
