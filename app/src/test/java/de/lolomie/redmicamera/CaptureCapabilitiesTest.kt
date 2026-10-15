package de.lolomie.redmicamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCapabilitiesTest {
    private val hd = CaptureSize(1280, 720)
    private val fhd = CaptureSize(1920, 1080)
    private val uhd = CaptureSize(3840, 2160)
    private val native200 = CaptureSize(16320, 12240)
    private val suppliedBack = CaptureCapabilities(
        cameraId = "0",
        jpegSizes = listOf(CaptureSize(4080, 3060)),
        regularAeFpsRanges = listOf(CaptureFpsRange(15, 30), CaptureFpsRange(30, 30)),
        regularVideoStreams = listOf(hd, fhd, uhd).map { VideoStreamEvidence(it, 33_333_333) },
        highSpeedStreams = listOf(
            HighSpeedStreamEvidence(fhd, listOf(CaptureFpsRange(30, 120), CaptureFpsRange(120, 120))),
            HighSpeedStreamEvidence(hd, listOf(CaptureFpsRange(120, 120), CaptureFpsRange(240, 240))),
        ),
    )

    @Test fun `supplied JPEG dimensions retain exact pixel counts`() {
        assertEquals(12_484_800L, suppliedBack.jpegSizes.single().pixels)
        assertEquals(12.4848, suppliedBack.jpegSizes.single().megapixels, 0.000001)
        assertEquals(5.038848, CaptureSize(2592, 1944).megapixels, 0.000001)
    }

    @Test fun `ordinary 30fps and UHD30 remain candidates without device verification`() {
        for (size in listOf(hd, fhd, uhd)) {
            val result = CaptureModePolicy.regularVideo(suppliedBack, size, 30, true)
            assertTrue(result.apiExposed)
            assertFalse(result.deviceVerified)
        }
    }

    @Test fun `highspeed 120 and capable encoder never enable ordinary 60fps`() {
        assertFalse(CaptureModePolicy.regularVideo(suppliedBack, fhd, 60, true).apiExposed)
        assertFalse(CaptureModePolicy.highSpeedVideo(suppliedBack, fhd, 60, true).apiExposed)
    }

    @Test fun `ordinary AE alone cannot override slow or unknown stream timing`() {
        val camera = suppliedBack.copy(regularAeFpsRanges = listOf(CaptureFpsRange(30, 60)))
        assertFalse(CaptureModePolicy.regularVideo(camera, fhd, 60, true).apiExposed)
        val unknown = camera.copy(regularVideoStreams = listOf(VideoStreamEvidence(fhd, 0)))
        assertFalse(CaptureModePolicy.regularVideo(unknown, fhd, 60, true).apiExposed)
        val fast = camera.copy(regularVideoStreams = listOf(VideoStreamEvidence(fhd, 16_666_667)))
        assertTrue(CaptureModePolicy.regularVideo(fast, fhd, 60, true).apiExposed)
        assertFalse(CaptureModePolicy.regularVideo(fast, fhd, 60, false).apiExposed)
    }

    @Test fun `highspeed needs exact fixed camera pair and exact encoder support`() {
        assertTrue(CaptureModePolicy.highSpeedVideo(suppliedBack, fhd, 120, true).apiExposed)
        assertTrue(CaptureModePolicy.highSpeedVideo(suppliedBack, hd, 240, true).apiExposed)
        assertFalse(CaptureModePolicy.highSpeedVideo(suppliedBack, fhd, 240, true).apiExposed)
        assertFalse(CaptureModePolicy.highSpeedVideo(suppliedBack, hd, 240, false).apiExposed)
        val variableOnly = suppliedBack.copy(highSpeedStreams = listOf(
            HighSpeedStreamEvidence(fhd, listOf(CaptureFpsRange(30, 120))),
        ))
        assertFalse(CaptureModePolicy.highSpeedVideo(variableOnly, fhd, 120, true).apiExposed)
        assertFalse(CaptureModePolicy.highSpeedVideo(suppliedBack, hd, 240, true).deviceVerified)
    }

    @Test fun `front HD120 does not imply front FHD120 or any ordinary60`() {
        val front = suppliedBack.copy(cameraId = "1", jpegSizes = listOf(CaptureSize(2592, 1944)),
            highSpeedStreams = listOf(HighSpeedStreamEvidence(hd, listOf(CaptureFpsRange(120, 120)))))
        assertTrue(CaptureModePolicy.highSpeedVideo(front, hd, 120, true).apiExposed)
        assertFalse(CaptureModePolicy.highSpeedVideo(front, fhd, 120, true).apiExposed)
        assertFalse(CaptureModePolicy.regularVideo(front, fhd, 60, true).apiExposed)
    }

    @Test fun `default pixel mode JPEG does not establish UHR pixel mode availability`() {
        assertFalse(CaptureModePolicy.ultraHighResolutionPhoto(suppliedBack).apiExposed)
        assertFalse(CaptureModePolicy.ultraHighResolutionPhoto(suppliedBack.copy(jpegSizes = listOf(native200))).apiExposed)
    }

    @Test fun `native200MP needs UHR capability map pixel request and matching sensor array`() {
        val camera = suppliedBack.copy(ultraHighResolutionSensor = true,
            maximumResolutionJpegSizes = listOf(native200), maximumResolutionSensorSize = native200,
            sensorPixelModeRequestAvailable = true)
        assertTrue(CaptureModePolicy.ultraHighResolutionPhoto(camera).apiExposed)
        assertFalse(CaptureModePolicy.ultraHighResolutionPhoto(camera).deviceVerified)
        for (incomplete in listOf(
            camera.copy(ultraHighResolutionSensor = false),
            camera.copy(maximumResolutionJpegSizes = emptyList()),
            camera.copy(sensorPixelModeRequestAvailable = false),
            camera.copy(maximumResolutionSensorSize = null),
            camera.copy(maximumResolutionSensorSize = CaptureSize(4080, 3060)),
            camera.copy(maximumResolutionJpegSizes = listOf(CaptureSize(4080, 3060))),
        )) assertFalse(CaptureModePolicy.ultraHighResolutionPhoto(incomplete).apiExposed)
    }
}
