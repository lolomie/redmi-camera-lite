package de.lolomie.redmicamera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Range
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Main-thread controller. CameraX owns camera threads; no continuous analysis pipeline. */
@androidx.annotation.OptIn(markerClass = [
    androidx.camera.camera2.interop.ExperimentalCamera2Interop::class,
    androidx.camera.core.ExperimentalCameraInfo::class,
])
class CameraController(private val context: Context) : DefaultLifecycleObserver {
    var mode by mutableStateOf(CaptureMode.PHOTO)
        private set
    var front by mutableStateOf(false)
        private set
    var wide by mutableStateOf(false)
        private set
    var wideAvailable by mutableStateOf(false)
        private set
    var ready by mutableStateOf(false)
        private set
    var recording by mutableStateOf(false)
        private set
    var finalizing by mutableStateOf(false)
        private set
    var takingPhoto by mutableStateOf(false)
        private set
    var torch by mutableStateOf(false)
        private set
    var flash by mutableStateOf(false)
        private set
    var hasFlash by mutableStateOf(false)
        private set
    var uhdAvailable by mutableStateOf(false)
        private set
    var uhd by mutableStateOf(false)
        private set
    var zoom by mutableFloatStateOf(0f)
        private set
    var status by mutableStateOf("Kamera wird vorbereitet …")
        private set
    private var provider: ProcessCameraProvider? = null
    private var owner: LifecycleOwner? = null
    private var view: PreviewView? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var boundPreview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var disposed = false
    private var wideCameraId: String? = null
    private val executor = ContextCompat.getMainExecutor(context)
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var watchingDisplay = false
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (view?.display?.displayId == displayId) {
                displayManager.getDisplay(displayId)?.let { updateRotation(it.rotation) }
            }
        }
    }
    val busy: Boolean get() = recording || finalizing || takingPhoto

    fun attach(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        owner = lifecycleOwner
        view = previewView
        lifecycleOwner.lifecycle.addObserver(this)
        if (!watchingDisplay) {
            displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
            watchingDisplay = true
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed) {
                try {
                    provider = future.get()
                    inspectWideCamera()
                    bind()
                } catch (failure: Exception) {
                    status = "Kamera nicht verfügbar: ${failure.javaClass.simpleName}"
                }
            }
        }, executor)
    }

    fun bind() {
        if (disposed || busy) return
        val cameraProvider = provider ?: return
        val lifecycleOwner = owner ?: return
        val previewView = view ?: return
        ready = false
        torch = false
        imageCapture = null
        boundPreview = null
        videoCapture = null
        camera?.cameraInfo?.zoomState?.removeObservers(lifecycleOwner)
        camera?.cameraInfo?.cameraState?.removeObservers(lifecycleOwner)
        camera?.cameraInfo?.torchState?.removeObservers(lifecycleOwner)
        camera = null
        cameraProvider.unbindAll()
        try {
            val selector = when {
                front -> CameraSelector.DEFAULT_FRONT_CAMERA
                wide -> CameraSelector.Builder().addCameraFilter { infos ->
                    infos.filter { Camera2CameraInfo.from(it).cameraId == wideCameraId }
                }.build()
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }
            if (!cameraProvider.hasCamera(selector)) {
                status = "Die ausgewählte Kamera ist nicht verfügbar."
                return
            }
            val info = cameraProvider.getCameraInfo(selector)
            hasFlash = info.hasFlashUnit()
            if (!hasFlash) flash = false
            val qualities = Recorder.getVideoCapabilities(info).getSupportedQualities(androidx.camera.core.DynamicRange.SDR)
            uhdAvailable = !front && !wide && qualities.contains(Quality.UHD)
            if (!uhdAvailable) uhd = false
            val rotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
            val preview = Preview.Builder().setTargetRotation(rotation).build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            boundPreview = preview
            val bound = if (mode == CaptureMode.PHOTO) {
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(rotation).build()
                capture.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                imageCapture = capture
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val quality = if (uhd) Quality.UHD else Quality.FHD
                if (!qualities.contains(quality)) {
                    status = "${if (uhd) "4K" else "1080p"} ist über CameraX nicht verfügbar."
                    return
                }
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
                val capture = VideoCapture.Builder(recorder).setTargetFrameRate(Range(30, 30))
                    .setTargetRotation(rotation).build()
                videoCapture = capture
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            }
            camera = bound
            hasFlash = bound.cameraInfo.hasFlashUnit()
            if (!hasFlash) flash = false
            bound.cameraInfo.zoomState.observe(lifecycleOwner) {
                if (camera === bound && !disposed) zoom = it.linearZoom
            }
            bound.cameraInfo.torchState.observe(lifecycleOwner) {
                if (camera === bound && !disposed) torch = it == androidx.camera.core.TorchState.ON
            }
            bound.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                if (camera !== bound || disposed) return@observe
                ready = state.type == androidx.camera.core.CameraState.Type.OPEN && state.error == null
                state.error?.let {
                    status = "Kamerafehler ${it.code}. Kamera erneut starten."
                }
            }
            status = if (mode == CaptureMode.PHOTO) "Foto bereit · native Standardauflösung" else "${if (uhd) "4K" else "1080p"} · Ziel 30 fps · Audio"
        } catch (failure: Exception) {
            ready = false
            status = "Kamerastart fehlgeschlagen: ${failure.javaClass.simpleName}. Erneut versuchen."
        }
    }

    /** Only separate CameraX-bindable logical cameras qualify, never hidden physical IDs. */
    private fun inspectWideCamera() {
        val cameraProvider = provider ?: return
        runCatching {
            val mainInfo = cameraProvider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)
            val mainId = Camera2CameraInfo.from(mainInfo).cameraId
            fun normalizedFocal(info: androidx.camera.core.CameraInfo): Float? {
                val details = Camera2CameraInfo.from(info)
                if (details.getCameraCharacteristic(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) return null
                val focal = details.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull() ?: return null
                val sensor = details.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
                return if (sensor.width > 0f && focal > 0f) focal / sensor.width else null
            }
            val main = normalizedFocal(mainInfo) ?: return@runCatching
            wideCameraId = cameraProvider.availableCameraInfos.mapNotNull { info ->
                val id = Camera2CameraInfo.from(info).cameraId
                val ratio = normalizedFocal(info)
                if (id != mainId && ratio != null && ratio < main * 0.85f) id to ratio else null
            }.minByOrNull { it.second }?.first
            wideAvailable = wideCameraId != null
        }
    }

    fun switchCamera() { if (!busy) { front = !front; wide = false; uhd = false; bind() } }
    fun switchWide() { if (!busy && !front && wideAvailable) { wide = !wide; uhd = false; bind() } }
    fun changeMode(value: CaptureMode) { if (!busy && mode != value) { mode = value; bind() } }
    fun changeQuality() { if (!busy && uhdAvailable) { uhd = !uhd; bind() } }
    /** Recordings retain their initial orientation; idle preview/capture follow display rotation. */
    fun updateRotation(rotation: Int) {
        if (busy || disposed) return
        boundPreview?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
    }
    fun toggleFlash() {
        if (ready && hasFlash && !busy) {
            flash = !flash
            imageCapture?.flashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        }
    }
    fun toggleTorch() {
        val current = camera ?: return
        if (hasFlash) {
            val requested = !torch
            val result = current.cameraControl.enableTorch(requested)
            result.addListener({
                if (camera === current && !disposed) {
                    runCatching { result.get() }.onFailure { status = "Dauerlicht nicht verfügbar." }
                }
            }, executor)
        }
    }
    fun changeZoom(value: Float) {
        val current = camera ?: return
        val result = current.cameraControl.setLinearZoom(value.coerceIn(0f, 1f))
        result.addListener({
            if (camera === current && !disposed) runCatching { result.get() }.onFailure { status = "Zoom nicht verfügbar." }
        }, executor)
    }
    fun focus(x: Float, y: Float) {
        val previewView = view ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        val current = camera ?: return
        val result = current.cameraControl.startFocusAndMetering(action)
        result.addListener({
            if (camera === current && !disposed) runCatching { result.get() }.onFailure { status = "Fokus an dieser Position nicht verfügbar." }
        }, executor)
    }

    fun takePhoto() {
        val capture = imageCapture ?: return
        if (!ready || busy) return
        takingPhoto = true
        status = "Foto wird gespeichert …"
        capture.targetRotation = view?.display?.rotation ?: android.view.Surface.ROTATION_0
        val values = mediaValues("IMG", "image/jpeg", "Pictures/Redmi Camera Lite")
        val options = ImageCapture.OutputFileOptions.Builder(context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
        try {
            capture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    takingPhoto = false
                    updateRotation(view?.display?.rotation ?: android.view.Surface.ROTATION_0)
                    status = "Foto in Pictures/Redmi Camera Lite gespeichert."
                }
                override fun onError(exception: ImageCaptureException) {
                    takingPhoto = false
                    updateRotation(view?.display?.rotation ?: android.view.Surface.ROTATION_0)
                    status = "Foto konnte nicht gespeichert werden (${exception.imageCaptureError})."
                }
            })
        } catch (failure: Exception) {
            takingPhoto = false
            status = "Fotoaufnahme fehlgeschlagen: ${failure.javaClass.simpleName}"
        }
    }

    @Suppress("MissingPermission")
    fun startRecording() {
        if (!RecordingGate.canStart(mode, owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true,
                disposed, ready, busy)) {
            if (ready && !busy) status = "Videoaufnahme im Vordergrund erneut starten."
            return
        }
        val capture = videoCapture ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status = "Für Video mit Ton ist die Mikrofonberechtigung erforderlich."
            return
        }
        try {
            capture.targetRotation = view?.display?.rotation ?: android.view.Surface.ROTATION_0
            val output = MediaStoreOutputOptions.Builder(context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(mediaValues("VID", "video/mp4", "Movies/Redmi Camera Lite")).build()
            recording = true
            status = "Aufnahme startet …"
            activeRecording = capture.output.prepareRecording(context, output).withAudioEnabled()
                .start(executor) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> status = "● Aufnahme mit Ton läuft"
                        is VideoRecordEvent.Finalize -> {
                            activeRecording?.close()
                            activeRecording = null
                            recording = false
                            finalizing = false
                            updateRotation(view?.display?.rotation ?: android.view.Surface.ROTATION_0)
                            status = if (event.hasError()) "Videoabschluss fehlgeschlagen (${event.error}); Datei prüfen."
                            else "Video in Movies/Redmi Camera Lite gespeichert."
                        }
                    }
                }
        } catch (failure: Exception) {
            activeRecording?.close()
            activeRecording = null
            recording = false
            finalizing = false
            status = "Video konnte nicht starten: ${failure.javaClass.simpleName}"
        }
    }

    fun stopRecording() {
        if (recording && !finalizing) {
            finalizing = true
            status = "Video wird abgeschlossen …"
            activeRecording?.stop()
        }
    }
    fun permissionDenied() { status = "Mikrofonzugriff abgelehnt. Fotos bleiben verfügbar." }
    override fun onStop(owner: LifecycleOwner) { stopRecording() }
    fun dispose() {
        disposed = true
        if (watchingDisplay) {
            displayManager.unregisterDisplayListener(displayListener)
            watchingDisplay = false
        }
        stopRecording()
        activeRecording?.close()
        camera?.cameraControl?.enableTorch(false)
        owner?.let {
            it.lifecycle.removeObserver(this)
            camera?.cameraInfo?.zoomState?.removeObservers(it)
            camera?.cameraInfo?.cameraState?.removeObservers(it)
            camera?.cameraInfo?.torchState?.removeObservers(it)
        }
        provider?.unbindAll()
        view = null
        owner = null
        camera = null
    }
    private fun mediaValues(prefix: String, mime: String, path: String) = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME,
            "${prefix}_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}")
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        put(MediaStore.MediaColumns.RELATIVE_PATH, path)
    }
}
