package de.lolomie.redmicamera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingGateTest {
    @Test fun `microphone grant may start ready foreground video`() {
        assertTrue(RecordingGate.canStart(CaptureMode.VIDEO, resumed = true,
            disposed = false, ready = true, busy = false))
    }

    @Test fun `microphone grant after switching to photo never starts hidden video`() {
        assertFalse(RecordingGate.canStart(CaptureMode.PHOTO, resumed = true,
            disposed = false, ready = true, busy = false))
    }

    @Test fun `microphone grant delivered while backgrounded never starts recording`() {
        assertFalse(RecordingGate.canStart(CaptureMode.VIDEO, resumed = false,
            disposed = false, ready = true, busy = false))
    }

    @Test fun `microphone grant to disposed screen cannot reopen camera`() {
        assertFalse(RecordingGate.canStart(CaptureMode.VIDEO, resumed = true,
            disposed = true, ready = true, busy = false))
    }

    @Test fun `camera error before microphone grant blocks capture`() {
        assertFalse(RecordingGate.canStart(CaptureMode.VIDEO, resumed = true,
            disposed = false, ready = false, busy = false))
    }

    @Test fun `duplicate start while saving or recording cannot replace active recording`() {
        assertFalse(RecordingGate.canStart(CaptureMode.VIDEO, resumed = true,
            disposed = false, ready = true, busy = true))
    }
}
