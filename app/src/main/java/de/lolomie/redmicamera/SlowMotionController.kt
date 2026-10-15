package de.lolomie.redmicamera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaRecorder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Range
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

data class SlowMotionMode(
    val cameraId: String,
    val label: String,
    val width: Int,
    val height: Int,
    val captureFps: Int,
    val playbackFps: Int = 30,
)

/** Independent Camera2 pipeline. Never attach while the CameraX screen owns a camera. */
@Suppress("DEPRECATION")
class SlowMotionController(context: Context) : DefaultLifecycleObserver {
    private val app = context.applicationContext
    private val manager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val main = Handler(app.mainLooper)
    private val thread = HandlerThread("slow-motion-camera").apply { start() }
    private val worker = Handler(thread.looper)
    var modes by mutableStateOf<List<SlowMotionMode>>(emptyList())
        private set
    var selectedMode by mutableStateOf<SlowMotionMode?>(null)
        private set
    var status by mutableStateOf("Zeitlupen-Fähigkeiten werden geprüft …")
        private set
    var ready by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var finalizing by mutableStateOf(false)
        private set
    var preparing by mutableStateOf(false)
        private set
    val busy: Boolean get() = preparing || recording || finalizing
    var lastSavedUri by mutableStateOf<Uri?>(null)
        private set
    var lastSavedMode by mutableStateOf<SlowMotionMode?>(null)
        private set
    var lastCaptureElapsedMillis by mutableLongStateOf(0L)
        private set
    var lastSensorSummary by mutableStateOf("")
        private set

    private var owner: LifecycleOwner? = null
    private var view: TextureView? = null
    @Volatile private var texture: SurfaceTexture? = null
    @Volatile private var foreground = false
    @Volatile private var disposed = false
    @Volatile private var epoch = 0
    @Volatile private var activeMode: SlowMotionMode? = null
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var recorder: MediaRecorder? = null
    private var output: Output? = null
    private var recorderStarted = false
    private var captureStartedAt = 0L
    private var firstSensorTimestamp = 0L
    private var lastSensorTimestamp = 0L
    private var sensorCallbacks = 0L
    private var opening = 0
    private val closingDevices = mutableSetOf<CameraDevice>()
    private val retiredSurfaces = mutableListOf<Surface>()
    private val retiredTextures = mutableListOf<SurfaceTexture>()
    private var reopenRequested = false
    private data class Output(val uri: Uri, val descriptor: ParcelFileDescriptor, val mode: SlowMotionMode)

    fun attach(lifecycleOwner: LifecycleOwner, textureView: TextureView) {
        check(owner == null) { "A slow-motion controller may only be attached once" }
        owner = lifecycleOwner
        view = textureView
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                texture = surface
                worker.post { requestOpen() }
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                activeMode?.let { transform(it) }
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                texture = null
                val accepted = worker.post {
                    retiredTextures += surface
                    closePipeline("Vorschau beendet.")
                    releaseRetiredIfIdle()
                }
                if (!accepted) surface.release() // Worker only exits after all devices close.
                return false // Release only after the producer sessions have been closed.
            }
        }
        if (textureView.isAvailable) texture = textureView.surfaceTexture
        lifecycleOwner.lifecycle.addObserver(this)
        worker.post {
            val found = runCatching { discoverModes() }.getOrElse { emptyList() }
            main.post {
                if (!disposed) {
                    modes = found
                    selectedMode = found.firstOrNull()
                    activeMode = selectedMode
                    status = if (found.isEmpty()) "BLOCKED: keine exakt passende High-Speed-Konfiguration mit Hardware-AVC-Encoder verfügbar."
                    else "Zeitlupe ohne Ton · API-Konfiguration verfügbar; Aufnahme noch nicht verifiziert."
                    worker.post { requestOpen() }
                }
            }
        }
    }

    fun selectMode(mode: SlowMotionMode) {
        if (disposed || busy || mode !in modes || mode == selectedMode) return
        selectedMode = mode
        activeMode = mode
        ready = false
        worker.post { closePipeline("Kamerawechsel …"); requestOpen() }
    }

    fun retry() {
        if (disposed || busy) return
        ready = false
        worker.post { closePipeline("Kamera wird erneut geöffnet …"); requestOpen() }
    }

    fun startRecording() {
        if (disposed || busy || !ready || !foreground ||
            owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ready = false
            status = "Kameraberechtigung fehlt."
            return
        }
        val mode = selectedMode ?: return
        preparing = true
        ready = false
        status = "High-Speed-Zielsitzung (${mode.captureFps} fps) wird vorbereitet …"
        worker.post { beginRecording(mode) }
    }

    fun stopRecording() {
        if (disposed || !busy || finalizing) return
        finalizing = true
        status = "Zeitlupe wird abgeschlossen …"
        worker.post {
            val outcome = finishRecording()
            if (!disposed && foreground && texture != null) configurePreview(outcome)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        foreground = true
        worker.post { requestOpen() }
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground = false
        ready = false
        worker.post { closePipeline("Kamera im Hintergrund freigegeben.") }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        foreground = false
        ready = false
        owner?.lifecycle?.removeObserver(this)
        owner = null
        // Keep the destruction listener until TextureView relinquishes its texture. It returns
        // false so producers close before the app releases the SurfaceTexture.
        view = null
        texture = null
        worker.post {
            closePipeline("Kamera freigegeben.")
            quitWhenClosed()
        }
    }

    /** Exact fixed high-speed ranges and independent hardware encoder throughput are prerequisites. */
    private fun discoverModes(): List<SlowMotionMode> {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter {
            it.isEncoder && it.isHardwareAccelerated && it.supportedTypes.any { type -> type.equals("video/avc", true) }
        }
        return manager.cameraIdList.flatMap { id ->
            runCatching {
                val info = manager.getCameraCharacteristics(id)
                val capabilities = info.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                if (CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO !in capabilities) return@runCatching emptyList()
                val map = info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return@runCatching emptyList()
                val facing = info.get(CameraCharacteristics.LENS_FACING)
                val label = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Frontkamera"
                    CameraCharacteristics.LENS_FACING_BACK -> "Rückkamera"
                    else -> "Kamera $id"
                }
                map.highSpeedVideoSizes.flatMap { size ->
                    map.getHighSpeedVideoFpsRangesFor(size).filter { range ->
                        range.lower == range.upper && range.upper in setOf(120, 240) &&
                            codecs.any { codec ->
                                runCatching {
                                    val caps = codec.getCapabilitiesForType("video/avc")
                                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface in caps.colorFormats &&
                                        caps.videoCapabilities.areSizeAndRateSupported(size.width, size.height, range.upper.toDouble())
                                }.getOrDefault(false)
                            }
                    }.map { SlowMotionMode(id, label, size.width, size.height, it.upper) }
                }
            }.getOrDefault(emptyList())
        }.distinct().sortedWith(compareBy<SlowMotionMode> { it.cameraId }.thenBy { it.captureFps }.thenByDescending { it.width })
    }

    @Suppress("MissingPermission")
    private fun requestOpen() {
        if (disposed || !foreground || texture == null || activeMode == null) return
        if (camera != null) return
        if (opening > 0 || closingDevices.isNotEmpty()) { reopenRequested = true; return }
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            publish { ready = false; status = "Kameraberechtigung fehlt." }
            return
        }
        val mode = activeMode ?: return
        val token = epoch
        opening++
        var awaitingOpen = true
        fun settled() { if (awaitingOpen) { opening--; awaitingOpen = false } }
        try {
            manager.openCamera(mode.cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    settled()
                    if (disposed || !foreground || token != epoch) { closeDevice(device); return }
                    camera = device
                    configurePreview()
                }
                override fun onDisconnected(device: CameraDevice) {
                    settled()
                    val current = camera === device || token == epoch
                    if (camera === device) camera = null
                    closeDevice(device)
                    if (current && !disposed) fail("Kamera wurde getrennt. Erneut versuchen.")
                }
                override fun onError(device: CameraDevice, error: Int) {
                    settled()
                    val current = camera === device || token == epoch
                    if (camera === device) camera = null
                    closeDevice(device)
                    if (current && !disposed) fail("Kamerafehler $error. Erneut versuchen.")
                }
                override fun onClosed(device: CameraDevice) {
                    closingDevices.remove(device)
                    releaseRetiredIfIdle()
                    if (reopenRequested && closingDevices.isEmpty() && opening == 0 && !disposed) {
                        reopenRequested = false
                        requestOpen()
                    }
                    quitWhenClosed()
                }
            }, worker)
        } catch (failure: Exception) {
            settled()
            fail("Kamera kann nicht geöffnet werden: ${failure.javaClass.simpleName}. Erneut versuchen.")
            quitWhenClosed()
        }
    }

    private fun configurePreview(outcome: String? = null) {
        val device = camera ?: return
        val mode = activeMode ?: return
        val surfaceTexture = texture ?: return
        if (disposed || !foreground) return
        val token = ++epoch
        try {
            session?.close()
            session = null
            surfaceTexture.setDefaultBufferSize(mode.width, mode.height)
            // The same texture/surface remains valid when moving between idle and recording.
            val surface = previewSurface ?: Surface(surfaceTexture).also { previewSurface = it }
            transform(mode)
            device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(configured: CameraCaptureSession) {
                    if (token != epoch || disposed || !foreground) { configured.close(); return }
                    try {
                        session = configured
                        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(surface) }.build()
                        configured.setRepeatingRequest(request, null, worker)
                        publish(token) { ready = true; preparing = false; finalizing = false; recording = false
                            status = outcome ?: "${mode.label}: ${mode.width}×${mode.height}, Aufnahme ${mode.captureFps} fps → Wiedergabe 30 fps, ohne Ton · PENDING DEVICE" }
                    } catch (failure: Exception) { fail("Vorschau fehlgeschlagen: ${failure.javaClass.simpleName}") }
                }
                override fun onConfigureFailed(configured: CameraCaptureSession) {
                    configured.close()
                    if (token == epoch && !disposed) fail("Vorschau-Konfiguration nicht verfügbar. Erneut versuchen.")
                }
            }, worker)
        } catch (failure: Exception) { fail("Vorschaustart fehlgeschlagen: ${failure.javaClass.simpleName}") }
    }

    private fun beginRecording(mode: SlowMotionMode) {
        if (disposed || !foreground || activeMode != mode || texture == null || camera == null) {
            publish { preparing = false; status = "Aufnahme im Vordergrund erneut starten." }
            return
        }
        val device = camera ?: return
        val preview = previewSurface ?: return fail("Vorschau fehlt. Erneut versuchen.")
        runCatching { session?.close() }
        session = null
        val token = ++epoch
        try {
            val uri = app.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "SLOW_${System.currentTimeMillis()}_${mode.captureFps}fps.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Redmi Camera Lite")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }) ?: error("MediaStore insert failed")
            val descriptor = try {
                app.contentResolver.openFileDescriptor(uri, "w") ?: error("MediaStore descriptor missing")
            } catch (failure: Exception) {
                app.contentResolver.delete(uri, null, null)
                throw failure
            }
            output = Output(uri, descriptor, mode)
            val target = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(app) else MediaRecorder()
            recorder = target
            target.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            target.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            target.setOutputFile(descriptor.fileDescriptor)
            target.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            target.setVideoSize(mode.width, mode.height)
            target.setVideoFrameRate(mode.playbackFps)
            target.setCaptureRate(mode.captureFps.toDouble())
            target.setVideoEncodingBitRate(if (mode.width >= 1920) 16_000_000 else 8_000_000)
            target.setOrientationHint(orientationHint(mode))
            target.setOnErrorListener { _, what, extra -> worker.post { if (token == epoch) fail("Recorder-Fehler $what/$extra; Aufnahme verworfen.") } }
            target.prepare()
            val recordSurface = target.surface
            device.createConstrainedHighSpeedCaptureSession(listOf(preview, recordSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(configured: CameraCaptureSession) {
                    if (token != epoch || disposed || !foreground) { configured.close(); return }
                    try {
                        val highSpeed = configured as CameraConstrainedHighSpeedCaptureSession
                        session = highSpeed
                        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            addTarget(preview)
                            addTarget(recordSurface)
                            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(mode.captureFps, mode.captureFps))
                        }.build()
                        firstSensorTimestamp = 0
                        lastSensorTimestamp = 0
                        sensorCallbacks = 0
                        highSpeed.setRepeatingBurst(highSpeed.createHighSpeedRequestList(request), object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                if (token != epoch || !recorderStarted) return
                                val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: return
                                if (sensorCallbacks == 0L) firstSensorTimestamp = timestamp
                                lastSensorTimestamp = timestamp
                                sensorCallbacks++
                            }
                        }, worker)
                        captureStartedAt = SystemClock.elapsedRealtime()
                        target.start()
                        recorderStarted = true
                        publish(token) { preparing = false; recording = true; finalizing = false
                            status = "● Zeitlupe: Ziel ${mode.captureFps} fps → 30 fps · ohne Ton" }
                    } catch (failure: Exception) { fail("High-Speed-Aufnahmestart fehlgeschlagen: ${failure.javaClass.simpleName}") }
                }
                override fun onConfigureFailed(configured: CameraCaptureSession) {
                    configured.close()
                    if (token == epoch && !disposed) fail("High-Speed-Sitzung vom Gerät abgelehnt; keine Aufnahme gespeichert.")
                }
            }, worker)
        } catch (failure: Exception) { fail("Zeitlupenstart fehlgeschlagen: ${failure.javaClass.simpleName}") }
    }

    /** Stop before releasing surfaces. Pending rows are only published following successful stop. */
    private fun finishRecording(): String? {
        val pending = output
        val target = recorder
        val elapsed = if (recorderStarted) SystemClock.elapsedRealtime() - captureStartedAt else 0L
        session?.let { runCatching { it.stopRepeating() }; runCatching { it.abortCaptures() } }
        val stopped = recorderStarted && runCatching { target?.stop() ?: error("Recorder missing") }.isSuccess
        recorderStarted = false
        runCatching { session?.close() }
        session = null
        runCatching { target?.reset() }
        runCatching { target?.release() }
        recorder = null
        runCatching { pending?.descriptor?.close() }
        output = null
        val valid = pending != null && stopped && runCatching {
            val size = app.contentResolver.openFileDescriptor(pending.uri, "r")?.use { it.statSize } ?: 0L
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(app, pending.uri)
                val duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                size > 1024 && duration > 0 && width == pending.mode.width && height == pending.mode.height
            } finally { metadata.release() }
        }.getOrDefault(false)
        val published = pending != null && valid && runCatching {
            app.contentResolver.update(pending.uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null) == 1
        }.getOrDefault(false)
        val removed = pending != null && !published && runCatching {
            app.contentResolver.delete(pending.uri, null, null) == 1
        }.getOrDefault(false)
        val summary = "Delivered Camera2 callbacks=$sensorCallbacks; firstSensorNs=$firstSensorTimestamp; lastSensorNs=$lastSensorTimestamp. Batched callbacks are not a full frame-count or FPS proof."
        val outcome = when {
            published -> "Zeitlupe gespeichert · Aufnahmezeit ${elapsed} ms inkl. Startlatenz; tatsächliche Frames noch prüfen."
            pending != null && removed -> "Aufnahme nicht gültig abgeschlossen; eigene unvollständige Datei entfernt."
            pending != null -> "Aufnahme ungültig; eigene ausstehende Datei konnte nicht entfernt werden."
            else -> null
        }
        // Saved-media evidence must survive a subsequent preview/session epoch change.
        main.post {
            if (disposed) return@post
            if (published) {
                val saved = requireNotNull(pending)
                lastSavedUri = saved.uri
                lastSavedMode = saved.mode
                lastCaptureElapsedMillis = elapsed
                lastSensorSummary = summary
                status = requireNotNull(outcome)
            } else if (pending != null) status = requireNotNull(outcome)
        }
        publish { preparing = false; recording = false; finalizing = false; ready = false }
        return outcome
    }

    private fun fail(message: String) {
        closePipeline(message)
        publish { ready = false; preparing = false; recording = false; finalizing = false; status = message }
    }

    private fun closePipeline(message: String) {
        ++epoch // Invalidate all session and recorder callbacks before releasing resources.
        reopenRequested = false
        finishRecording()
        runCatching { session?.close() }
        session = null
        previewSurface?.let { retiredSurfaces += it }
        previewSurface = null
        camera?.let { camera = null; closeDevice(it) }
        releaseRetiredIfIdle()
        publish { ready = false; preparing = false; recording = false; finalizing = false
            if (lastSavedUri == null) status = message }
    }

    private fun closeDevice(device: CameraDevice) {
        if (closingDevices.add(device)) runCatching { device.close() }.onFailure { closingDevices.remove(device) }
        quitWhenClosed()
    }

    private fun quitWhenClosed() {
        releaseRetiredIfIdle()
        if (disposed && camera == null && opening == 0 && closingDevices.isEmpty()) thread.quitSafely()
    }

    private fun releaseRetiredIfIdle() {
        if (camera != null || opening > 0 || closingDevices.isNotEmpty()) return
        retiredSurfaces.forEach { runCatching { it.release() } }
        retiredSurfaces.clear()
        retiredTextures.forEach { runCatching { it.release() } }
        retiredTextures.clear()
    }

    private fun publish(token: Int = epoch, block: () -> Unit) {
        main.post { if (token == epoch && !disposed) block() }
    }

    private fun orientationHint(mode: SlowMotionMode): Int {
        val info = manager.getCameraCharacteristics(mode.cameraId)
        val sensor = info.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val rotation = when (view?.display?.rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        return if (info.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
            (sensor + rotation) % 360 else (sensor - rotation + 360) % 360
    }

    private fun transform(mode: SlowMotionMode) {
        main.post {
            val preview = view ?: return@post
            if (disposed || preview.width == 0 || preview.height == 0) return@post
            val rotation = preview.display?.rotation ?: Surface.ROTATION_0
            val viewRect = RectF(0f, 0f, preview.width.toFloat(), preview.height.toFloat())
            val bufferRect = RectF(0f, 0f, mode.height.toFloat(), mode.width.toFloat())
            val matrix = Matrix()
            val x = viewRect.centerX()
            val y = viewRect.centerY()
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                bufferRect.offset(x - bufferRect.centerX(), y - bufferRect.centerY())
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                val scale = maxOf(preview.height.toFloat() / mode.height, preview.width.toFloat() / mode.width)
                matrix.postScale(scale, scale, x, y)
                matrix.postRotate(90f * (rotation - 2), x, y)
            } else if (rotation == Surface.ROTATION_180) matrix.postRotate(180f, x, y)
            preview.setTransform(matrix)
        }
    }
}
