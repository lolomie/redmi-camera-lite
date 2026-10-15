package de.lolomie.redmicamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,
        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var diagnostics by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var loadingDiagnostics by remember { mutableStateOf(false) }
    var cameraBusy by remember { mutableStateOf(false) }
    var slowMotion by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (slowMotion) "Zeitlupe · API-Test" else "Redmi Camera Lite", Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            if (granted) {
                TextButton(enabled = !cameraBusy && !loadingDiagnostics,
                    onClick = { slowMotion = !slowMotion }) {
                    Text(if (slowMotion) "Kamera" else "Zeitlupe")
                }
            }
            TextButton(enabled = !loadingDiagnostics && !cameraBusy, onClick = {
                loadingDiagnostics = true
                scope.launch {
                    diagnostics = withContext(Dispatchers.IO) {
                        runCatching { Diagnostics.collect(context) }
                            .getOrElse { "Diagnose fehlgeschlagen: ${it.javaClass.simpleName}" }
                    }
                    loadingDiagnostics = false
                }
            }) { Text(if (loadingDiagnostics) "Lädt …" else "Diagnose") }
        }
        if (granted) {
            if (slowMotion) {
                SlowMotionScreen(Modifier.weight(1f), loadingDiagnostics,
                    onBusy = { cameraBusy = it }, onExit = { slowMotion = false })
            } else {
                CameraContent(Modifier.weight(1f), loadingDiagnostics) { cameraBusy = it }
            }
        } else {
            Text("Kamerazugriff ist für Vorschau und Aufnahmen erforderlich. Mikrofonzugriff wird erst für Video mit Ton angefragt.")
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Kamerazugriff erlauben") }
            Text("Falls dauerhaft abgelehnt: Berechtigungen in den Android-App-Einstellungen aktivieren.")
        }
    }
    diagnostics?.let { report ->
        AlertDialog(onDismissRequest = { diagnostics = null }, title = { Text("Geräte-Diagnose") },
            text = { Text(report, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Redmi Camera Lite Diagnose")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, "Diagnose bewusst teilen")) }
                }) { Text("Teilen") }
            }, dismissButton = { TextButton(onClick = { diagnostics = null }) { Text("Schließen") } })
    }
}

@Composable
private fun CameraContent(modifier: Modifier, diagnosticsRunning: Boolean, onBusy: (Boolean) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context.applicationContext) }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) controller.startRecording() else controller.permissionDenied()
    }
    DisposableEffect(controller) { onDispose { controller.dispose(); onBusy(false) } }
    LaunchedEffect(controller.busy) { onBusy(controller.busy) }
    val startVideo = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            controller.startRecording()
        } else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Keep the same AndroidView node across orientation changes, including active recording.
    Layout(modifier = modifier, content = {
        CameraPreview(controller, owner, Modifier.fillMaxSize())
        CameraControls(controller, diagnosticsRunning, startVideo,
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()))
    }) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gap = 8.dp.roundToPx()
        if (landscape) {
            val controlsWidth = 280.dp.roundToPx().coerceAtMost(width / 2)
            val previewWidth = (width - controlsWidth - gap).coerceAtLeast(0)
            val preview = measurables[0].measure(Constraints.fixed(previewWidth, height))
            val controls = measurables[1].measure(Constraints.fixed(controlsWidth, height))
            layout(width, height) {
                preview.placeRelative(0, 0)
                controls.placeRelative(previewWidth + gap, 0)
            }
        } else {
            val controls = measurables[1].measure(Constraints(minWidth = width, maxWidth = width,
                maxHeight = (height * 0.6f).toInt()))
            val previewHeight = (height - controls.height - gap).coerceAtLeast(0)
            val preview = measurables[0].measure(Constraints.fixed(width, previewHeight))
            layout(width, height) {
                preview.placeRelative(0, 0)
                controls.placeRelative(0, previewHeight + gap)
            }
        }
    }
}

@Composable
private fun CameraPreview(controller: CameraController, owner: LifecycleOwner, modifier: Modifier) {
    AndroidView(modifier = modifier, factory = { previewContext ->
        PreviewView(previewContext).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    view.performClick()
                    controller.focus(event.x, event.y)
                }
                true
            }
            controller.attach(owner, this)
        }
    }, update = { preview ->
        preview.post { controller.updateRotation(preview.display?.rotation ?: android.view.Surface.ROTATION_0) }
    })
}

@Composable
private fun CameraControls(controller: CameraController, diagnosticsRunning: Boolean,
    startVideo: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(controller.status, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        if (!controller.hasAutoFocus) Text("Kamera mit festem Fokus · Tippen misst nur die Belichtung, soweit unterstützt.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CaptureMode.entries.forEach { mode ->
                FilterChip(selected = controller.mode == mode, enabled = !controller.busy,
                    onClick = { controller.changeMode(mode) }, label = { Text(if (mode == CaptureMode.PHOTO) "Foto" else "Video") })
            }
            OutlinedButton(enabled = !controller.busy, onClick = { controller.switchCamera() }) {
                Text(if (controller.front) "Hauptkamera" else "Frontkamera")
            }
        }
        if (controller.wideAvailable && !controller.front) {
            FilterChip(selected = controller.wide, enabled = !controller.busy,
                onClick = { controller.switchWide() }, label = { Text(if (controller.wide) "Weitwinkel · API verfügbar" else "Weitwinkel-Kandidat wählen") })
            if (controller.wide) Text("Größeres API-Sichtfeld; Sensor und Aufnahmequalität am Gerät prüfen.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        if (controller.mode == CaptureMode.VIDEO) {
            Text("Video mit Ton · Ziel 30 fps; tatsächliche Bildrate am Gerät prüfen.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            if (controller.uhdAvailable) {
                FilterChip(selected = controller.uhd, enabled = !controller.busy,
                    onClick = { controller.changeQuality() }, label = { Text(if (controller.uhd) "4K · API verfügbar" else "1080p · 4K verfügbar") })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Zoom")
            if (controller.hasFlash) {
                if (controller.mode == CaptureMode.PHOTO) {
                    TextButton(enabled = !controller.busy, onClick = { controller.toggleFlash() }) {
                        Text(if (controller.flash) "Blitz an" else "Blitz aus")
                    }
                }
                TextButton(onClick = { controller.toggleTorch() }) { Text(if (controller.torch) "Licht an" else "Licht aus") }
            }
        }
        Slider(value = controller.zoom, enabled = controller.ready && !controller.finalizing,
            onValueChange = { controller.changeZoom(it) }, valueRange = 0f..1f)
        Button(modifier = Modifier.fillMaxWidth(), enabled = !controller.takingPhoto && !controller.finalizing &&
            (controller.recording || controller.ready && !diagnosticsRunning),
            onClick = {
                when {
                    controller.mode == CaptureMode.PHOTO -> controller.takePhoto()
                    controller.recording -> controller.stopRecording()
                    else -> startVideo()
                }
            }) { Text(when {
                controller.finalizing -> "Video wird gespeichert …"
                controller.takingPhoto -> "Foto wird gespeichert …"
                controller.recording -> "■ Aufnahme stoppen"
                controller.mode == CaptureMode.PHOTO -> "Foto aufnehmen"
                else -> "● Video mit Ton aufnehmen"
            }) }
        if (!controller.ready) {
            OutlinedButton(enabled = !controller.busy, onClick = { controller.bind() }) { Text("Kamera erneut starten") }
        }
    }
}
