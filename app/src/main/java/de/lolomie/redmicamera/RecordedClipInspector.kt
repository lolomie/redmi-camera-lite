package de.lolomie.redmicamera

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build

/** Bounded post-capture file inspection. Invoke only off the main thread. */
object RecordedClipInspector {
    fun inspect(context: Context, uri: Uri, mode: SlowMotionMode, elapsedMillis: Long): String {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val tracks = (0 until extractor.trackCount).map { extractor.getTrackFormat(it) }
            val videoIndex = tracks.indexOfFirst { it.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
            check(videoIndex >= 0) { "No video track" }
            val video = tracks[videoIndex]
            val width = video.getInteger(MediaFormat.KEY_WIDTH)
            val height = video.getInteger(MediaFormat.KEY_HEIGHT)
            val hasAudio = tracks.any { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            extractor.selectTrack(videoIndex)
            val pts = mutableListOf<Long>()
            val limit = 300_000
            while (extractor.sampleTime >= 0 && pts.size < limit) {
                pts += extractor.sampleTime
                if (!extractor.advance()) break
            }
            val truncated = pts.size >= limit
            val ordered = pts.sorted()
            val span = if (ordered.size > 1) ordered.last() - ordered.first() else 0L
            val gaps = ordered.zipWithNext { a, b -> b - a }
            val fileFps = if (span > 0) (ordered.size - 1) * 1_000_000.0 / span else 0.0
            val captureEstimate = if (elapsedMillis > 0) ordered.size * 1_000.0 / elapsedMillis else 0.0
            val checks = ClipTimingAssessment.assess(width, height, mode.width, mode.height,
                mode.captureFps, mode.playbackFps, elapsedMillis, pts, hasAudio, truncated)
            var decoded = false
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                    decoded = true
                    it.recycle()
                }
            } finally { retriever.release() }
            return buildString {
                appendLine("Redmi Camera Lite — Zeitlupen-Dateiprüfung")
                appendLine("Modell=${Build.MODEL}; API=${Build.VERSION.SDK_INT}; Patch=${Build.VERSION.SECURITY_PATCH}")
                appendLine("Modus: Kamera ${mode.cameraId}, ${mode.width}x${mode.height}, Ziel-Aufnahme ${mode.captureFps} fps, Ziel-Wiedergabe ${mode.playbackFps} fps")
                appendLine("CAPTURED: eine gespeicherte MP4-Datei untersucht. DEVICE_VERIFIED: weiterhin offen.")
                appendLine("Gespeichert: ${width}x${height}; Codec=${video.getString(MediaFormat.KEY_MIME)}; Audio=$hasAudio; Frame decodiert=$decoded")
                appendLine("Frames=${pts.size}; Scan abgeschnitten=$truncated; PTS-SpanneUs=$span; größte LückeUs=${gaps.maxOrNull()}")
                appendLine("Datei-Wiedergabe-FPS=$fileFps; monotone reale AufnahmedauerMs=$elapsedMillis")
                appendLine("Schätzung Frames/reale Zeit=$captureEstimate fps. Recorder-Startlatenz ist mögliche Messunsicherheit.")
                checks.forEach { (name, passed) -> appendLine("$name=$passed") }
                appendLine("file_criteria_passed=${checks.values.all { it } && decoded}")
                appendLine("Dateikriterien beweisen keine unterschiedlichen nativen Sensorframes. Ein bewegtes Motiv, mögliche Duplikate, Kameratimestamps, Bildqualität und Dauerstabilität separat prüfen.")
                appendLine("Keine Seriennummer, kein Fingerprint und keine Medieninhalte im Bericht.")
            }
        } finally { extractor.release() }
    }
}
