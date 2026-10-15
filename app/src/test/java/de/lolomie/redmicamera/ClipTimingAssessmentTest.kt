package de.lolomie.redmicamera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipTimingAssessmentTest {
    private fun pts(count: Int, playback: Int = 30) = List(count) { it * 1_000_000L / playback }
    private fun assess(count: Int = 600, capture: Int = 120, wall: Long = 5_000,
        times: List<Long> = pts(count), audio: Boolean = false, truncated: Boolean = false) =
        ClipTimingAssessment.assess(1920, 1080, 1920, 1080, capture, 30, wall, times, audio, truncated)

    @Test fun `120 capture slowed to 30 passes timing criteria`() {
        assertTrue(assess().values.all { it })
    }
    @Test fun `240 capture slowed to 30 passes timing criteria`() {
        assertTrue(assess(count = 1_200, capture = 240).values.all { it })
    }
    @Test fun `ordinary30 relabeled120 fails real time frame count`() {
        assertFalse(assess(count = 150)["frame_count_vs_wall_time"]!!)
    }
    @Test fun `native120 playback is not30 slow motion`() {
        assertFalse(assess(times = pts(600, 120))["playback_fps"]!!)
    }
    @Test fun `duplicated timestamps and missing frame fail pacing`() {
        val times = pts(600).toMutableList()
        times[1] = times[0]
        assertFalse(assess(times = times)["unique_positive_intervals"]!!)
        assertFalse(assess(times = pts(600).filterIndexed { index, _ -> index != 100 })["playback_pacing"]!!)
    }
    @Test fun `audio and truncated scan cannot silently pass`() {
        assertFalse(assess(audio = true)["no_audio"]!!)
        assertFalse(assess(truncated = true)["complete_scan"]!!)
    }
    @Test fun `short capture and missing real timing cannot pass`() {
        assertFalse(assess(count = 240, wall = 2_000)["minimum_real_capture_4s"]!!)
        assertFalse(assess(wall = 0)["frame_count_vs_wall_time"]!!)
    }
}
