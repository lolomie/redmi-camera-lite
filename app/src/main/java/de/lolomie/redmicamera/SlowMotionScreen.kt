package de.lolomie.redmicamera

import android.content.Intent
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SlowMotionScreen(modifier: Modifier, diagnosticsRunning: Boolean,
    onBusy: (Boolean) -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { SlowMotionController(context.applicationContext) }
    var report by remember { mutableStateOf<String?>(null) }
    var inspecting by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    DisposableEffect(controller) { onDispose { controller.dispose(); onBusy(false) } }
    LaunchedEffect(controller.busy, inspecting) { onBusy(controller.busy || inspecting) }
    LaunchedEffect(controller.lastSavedUri) {
        val uri = controller.lastSavedUri ?: return@LaunchedEffect
        val mode = controller.lastSavedMode ?: return@LaunchedEffect
        inspecting = true
        try {
            report = withContext(Dispatchers.IO) {
                runCatching { RecordedClipInspector.inspect(context, uri, mode,
                    controller.lastCaptureElapsedMillis) + "\n" + controller.lastSensorSummary }
                    .getOrElse { "Dateiprüfung fehlgeschlagen: ${it.javaClass.simpleName}" }
            }
        } finally { inspecting = false }
    }
    BackHandler { if (controller.busy) controller.stopRecording() else if (!inspecting) onExit() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Öffentliche High-Speed-Session · experimenteller Aufnahmetest. Kein Ton.")
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val selected = controller.selectedMode
            val ratio = if (selected == null) 9f / 16f else if (landscape)
                selected.width.toFloat() / selected.height else selected.height.toFloat() / selected.width
            val previewWidth = minOf(maxWidth, maxHeight * ratio)
            AndroidView(modifier = Modifier.width(previewWidth).height(previewWidth / ratio), factory = { viewContext ->
                TextureView(viewContext).also { controller.attach(owner, it) }
            })
        }
        Column(Modifier.fillMaxWidth().heightIn(max = 250.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(controller.status)
            controller.modes.forEach { mode ->
                FilterChip(selected = mode == controller.selectedMode,
                    enabled = !controller.busy && !inspecting && !diagnosticsRunning,
                    onClick = { controller.selectMode(mode) },
                    label = { Text("${mode.label} · ${mode.width}×${mode.height} · ${mode.captureFps} fps") })
            }
            Text("Ziel-Wiedergabe: 30 fps; 120 fps → 4×, 240 fps → 8× langsamer. Erst Datei und reales Motiv prüfen.")
            if (inspecting) Text("Gespeicherte Datei wird geprüft …")
            Button(modifier = Modifier.fillMaxWidth(),
                enabled = !controller.finalizing && (controller.recording ||
                    (controller.ready && !controller.busy && !inspecting && !diagnosticsRunning)),
                onClick = { if (controller.recording) controller.stopRecording() else controller.startRecording() }) {
                Text(when {
                    controller.finalizing -> "Zeitlupe wird gespeichert …"
                    controller.recording -> "Zeitlupe stoppen"
                    controller.busy -> "Zeitlupe startet …"
                    else -> "Zeitlupen-Test ohne Ton aufnehmen"
                })
            }
            if (!controller.ready && !controller.busy) {
                OutlinedButton(onClick = { controller.retry() }) { Text("Kamera erneut prüfen") }
            }
            if (report != null) TextButton(enabled = !controller.busy && !inspecting,
                onClick = { showReport = true }) { Text("Letzte Aufnahme prüfen / Bericht teilen") }
        }
    }
    if (showReport) AlertDialog(onDismissRequest = { showReport = false },
        title = { Text("Zeitlupen-Dateiprüfung") },
        text = { Text(report.orEmpty(), Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Redmi Camera Lite Zeitlupen-Dateiprüfung")
                putExtra(Intent.EXTRA_TEXT, report)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, "Prüfbericht bewusst teilen")) }
        }) { Text("Teilen") } },
        dismissButton = { TextButton(onClick = { showReport = false }) { Text("Schließen") } })
}
