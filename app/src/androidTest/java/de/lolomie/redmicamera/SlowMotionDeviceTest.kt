package de.lolomie.redmicamera

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import android.util.Size
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real-device tests. Retains generated clips. No microphone permission is granted. */
@RunWith(AndroidJUnit4::class)
class SlowMotionDeviceTest {
    @get:Rule val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)

    @Test fun rearFhd120() = capture(false, 1920, 1080, 120)
    @Test fun rearHd120() = capture(false, 1280, 720, 120)
    @Test fun rearHd240() = capture(false, 1280, 720, 240)
    @Test fun frontHd120() = capture(true, 1280, 720, 120)

    private fun capture(front: Boolean, width: Int, height: Int, fps: Int) {
        val manager = instrumentation.targetContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val facing = if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        val exposed = manager.cameraIdList.any { id ->
            val c = manager.getCameraCharacteristics(id)
            val map = c[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            c[CameraCharacteristics.LENS_FACING] == facing &&
                c[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]?.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) == true &&
                map?.highSpeedVideoSizes?.contains(Size(width, height)) == true &&
                map.getHighSpeedVideoFpsRangesFor(Size(width, height)).any { it.lower == fps && it.upper == fps }
        }
        assumeTrue("Requested constrained-high-speed size/FPS is not API-exposed", exposed)
        device.wakeUp()
        device.setOrientationNatural()
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                control("Zeitlupe").click()
                val label = "${if (front) "Frontkamera" else "Rückkamera"} · ${width}×${height} · $fps fps"
                control(label).click()
                control("Zeitlupen-Test ohne Ton aufnehmen").click()
                assertNotNull("Actual high-speed session must start", device.wait(Until.findObject(
                    By.text("● Zeitlupe: Ziel $fps fps → 30 fps · ohne Ton")), TIMEOUT))
                SystemClock.sleep(5_000)
                control("Zeitlupe stoppen").click()
                control("Letzte Aufnahme prüfen / Bericht teilen").click()
                assertNotNull("Saved video must satisfy measured dimensions, no audio, PTS and real-time frame-count criteria",
                    device.wait(Until.findObject(By.textContains("file_criteria_passed=true")), TIMEOUT))
                control("Schließen").click()
                control("Kamera").click()
                control("Foto aufnehmen") // The ordinary CameraX pipeline must reopen.
            }
        } finally { device.unfreezeRotation() }
    }

    private fun control(text: String): UiObject2 {
        var found = device.wait(Until.findObject(By.text(text).enabled(true)), 2_000)
        if (found == null) {
            runCatching { UiScrollable(UiSelector().scrollable(true)).scrollTextIntoView(text) }
            found = device.wait(Until.findObject(By.text(text).enabled(true)), TIMEOUT)
        }
        return requireNotNull(found) { "Enabled control missing: $text; inspect camera/encoder errors and device report" }
    }

    companion object { private const val TIMEOUT = 30_000L }
}
