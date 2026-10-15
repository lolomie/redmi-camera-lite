package de.lolomie.redmicamera

/** File criteria, not proof of distinct sensor frames or a device certification. */
internal object ClipTimingAssessment {
    fun assess(width: Int, height: Int, expectedWidth: Int, expectedHeight: Int,
        requestedCaptureFps: Int, playbackFps: Int, elapsedMillis: Long,
        presentationTimesUs: List<Long>, hasAudio: Boolean, truncated: Boolean = false): Map<String, Boolean> {
        val ordered = presentationTimesUs.sorted()
        val gaps = ordered.zipWithNext { a, b -> b - a }
        val span = if (ordered.size > 1) ordered.last() - ordered.first() else 0L
        val fileFps = if (span > 0) (ordered.size - 1) * 1_000_000.0 / span else 0.0
        val estimatedCaptureFps = if (elapsedMillis > 0) ordered.size * 1_000.0 / elapsedMillis else 0.0
        return linkedMapOf(
            "dimensions" to (setOf(width, height) == setOf(expectedWidth, expectedHeight)),
            "complete_scan" to !truncated,
            "minimum_real_capture_4s" to (elapsedMillis >= 4_000L),
            "unique_positive_intervals" to (gaps.isNotEmpty() && gaps.all { it > 0L }),
            "playback_fps" to (playbackFps > 0 && kotlin.math.abs(fileFps - playbackFps) <= playbackFps * 0.05),
            "playback_pacing" to (playbackFps > 0 && gaps.isNotEmpty() && gaps.all { it > 0L && it <= 1_500_000.0 / playbackFps }),
            "frame_count_vs_wall_time" to (requestedCaptureFps > 0 && elapsedMillis > 0 &&
                kotlin.math.abs(estimatedCaptureFps - requestedCaptureFps) <= requestedCaptureFps * 0.10),
            "no_audio" to !hasAudio,
        )
    }
}
