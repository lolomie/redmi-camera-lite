package de.lolomie.redmicamera

enum class CaptureMode { PHOTO, VIDEO }

/** Re-evaluate when a permission result arrives; the request-time state may be stale. */
internal object RecordingGate {
    fun canStart(mode: CaptureMode, resumed: Boolean, disposed: Boolean, ready: Boolean, busy: Boolean): Boolean =
        mode == CaptureMode.VIDEO && resumed && !disposed && ready && !busy
}
