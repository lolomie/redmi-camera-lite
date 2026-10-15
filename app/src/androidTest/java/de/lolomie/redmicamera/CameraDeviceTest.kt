package de.lolomie.redmicamera

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real hardware acceptance checks, never an emulator substitute for Redmi validation.
 * Keeps generated media for inspection. Run with an unlocked device and adequate lighting.
 * Passing establishes these particular samples only, not 60 fps, 200 MP or slow motion.
 */
@RunWith(AndroidJUnit4::class)
class CameraDeviceTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test fun rearPhotoAndFhd30AudioVideoSurviveRotation() {
        exerciseCamera(front = false, rotateDuringRecording = true)
    }

    @Test fun frontPhotoAndFhd30VideoHaveRealAudioAndFrameTimestamps() {
        exerciseCamera(front = true, rotateDuringRecording = false)
    }

    private fun exerciseCamera(front: Boolean, rotateDuringRecording: Boolean) {
        val facing = if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        assumeTrue("BLOCKED: requested camera is not exposed on this device", manager.cameraIdList.any {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == facing
        })
        Log.i(TAG, "BEGIN real-device acceptance: ${if (front) "front" else "rear"}; FHD target30; rotate=$rotateDuringRecording")
        Log.i(TAG, Diagnostics.collect(context))
        val oldPhotos = mediaIds(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "IMG", "Pictures/Redmi Camera Lite/")
        val oldVideos = mediaIds(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "VID", "Movies/Redmi Camera Lite/")
        device.wakeUp()
        device.setOrientationNatural()
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                if (front) button("Frontkamera").click()
                button("Foto aufnehmen").click()
                text("Foto in Pictures/Redmi Camera Lite gespeichert.")
                val photo = newMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, oldPhotos,
                    "IMG", "Pictures/Redmi Camera Lite/")
                verifyPhoto(photo)
                button("Video").click()
                button("● Video mit Ton aufnehmen").click()
                text("● Aufnahme mit Ton läuft")
                SystemClock.sleep(2_000)
                if (rotateDuringRecording) {
                    device.setOrientationLeft()
                    text("● Aufnahme mit Ton läuft")
                }
                SystemClock.sleep(3_000)
                button("■ Aufnahme stoppen").click()
                text("Video in Movies/Redmi Camera Lite gespeichert.")
                val video = newMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, oldVideos,
                    "VID", "Movies/Redmi Camera Lite/")
                verifyVideo(video)
                Log.i(TAG, "CAPTURED: photo=$photo video=$video; inspect retained files and full report before DEVICE_VERIFIED designation")
            }
        } finally {
            device.unfreezeRotation()
        }
    }

    private fun text(value: String): UiObject2 = requireNotNull(device.wait(Until.findObject(By.text(value)), TIMEOUT)) {
        "Expected camera state missing: $value. Check device logcat and the API capability report."
    }

    private fun button(value: String): UiObject2 = requireNotNull(device.wait(
        Until.findObject(By.text(value).enabled(true)), TIMEOUT)) {
        "Enabled control missing: $value. Mode may be BLOCKED by this device/API combination."
    }

    private fun mediaIds(collection: Uri, prefix: String, path: String): Set<Long> {
        val ids = mutableSetOf<Long>()
        context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf("${prefix}_%", path), null)?.use { cursor ->
            while (cursor.moveToNext()) ids += cursor.getLong(0)
        }
        return ids
    }

    private fun newMedia(collection: Uri, before: Set<Long>, prefix: String, path: String): Uri {
        val created = mediaIds(collection, prefix, path) - before
        assertEquals("Exactly one new app-owned media item must be saved", 1, created.size)
        return ContentUris.withAppendedId(collection, created.single())
    }

    private fun verifyPhoto(uri: Uri) {
        assertEquals("image/jpeg", context.contentResolver.getType(uri))
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { stream ->
            assertNotNull("Saved JPEG must be readable", stream)
            BitmapFactory.decodeStream(stream, null, options)
        }
        assertTrue("Real JPEG width", options.outWidth > 0)
        assertTrue("Real JPEG height", options.outHeight > 0)
        Log.i(TAG, "CAPTURED JPEG ${options.outWidth}x${options.outHeight}; standard resolution, no 200MP claim")
    }

    private fun verifyVideo(uri: Uri) {
        val metadata = MediaMetadataRetriever()
        try {
            metadata.setDataSource(context, uri)
            val width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            assertTrue("Actual saved video must be 1920x1080 (rotation may swap dimensions)",
                width != null && height != null && setOf(width, height) == setOf(1920, 1080))
            val duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            assertTrue("Five-second capture must survive lifecycle/rotation and contain >=4 seconds", duration >= 4_000)
            val decodedFrame = metadata.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            assertNotNull("An actual encoded video frame must decode", decodedFrame)
            decodedFrame?.recycle()
            Log.i(TAG, "CAPTURED video ${width}x${height}, duration=${duration}ms, rotation=${metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)}")
        } finally {
            metadata.release()
        }
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val videoTrack = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            }
            val audioTrack = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            assertNotNull("Real video track required", videoTrack)
            assertNotNull("Real microphone audio track required", audioTrack)
            extractor.selectTrack(requireNotNull(videoTrack))
            val pts = mutableListOf<Long>()
            while (extractor.sampleTime >= 0 && pts.size < 100_000) {
                pts += extractor.sampleTime
                if (!extractor.advance()) break
            }
            assertTrue("Expected >=100 encoded frames for five-second 30 fps sample", pts.size >= 100)
            assertTrue("Video sample scan must terminate", pts.size < 100_000)
            // Decode order may differ from presentation order with B-frames.
            val ordered = pts.sorted()
            val intervals = ordered.zipWithNext { a, b -> b - a }
            assertTrue("Presentation timestamps must be unique", intervals.all { it > 0 })
            val span = ordered.last() - ordered.first()
            val effectiveFps = (ordered.size - 1) * 1_000_000.0 / span
            val longGaps = intervals.count { it > 50_000 }
            val maxGap = intervals.maxOrNull() ?: Long.MAX_VALUE
            Log.i(TAG, "CAPTURED PTS frames=${pts.size}; effectiveFps=$effectiveFps; maxGapUs=$maxGap; gapsAbove50ms=$longGaps/${intervals.size}")
            assertTrue("Actual PTS average must be 28.5–31.5 fps; was $effectiveFps", effectiveFps in 28.5..31.5)
            assertTrue("No sustained frame loss: >=95% presentation intervals <=50ms", longGaps <= intervals.size * 0.05)
            assertTrue("No presentation gap >100ms", maxGap <= 100_000)
            extractor.unselectTrack(videoTrack)
            extractor.selectTrack(requireNotNull(audioTrack))
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            assertTrue("Audio track must contain encoded samples", extractor.sampleTime >= 0 && extractor.sampleSize > 0)
            assertTrue("Audio must contain more than one sample", extractor.advance())
        } finally {
            extractor.release()
        }
    }

    companion object {
        private const val TAG = "CameraDeviceTest"
        private const val TIMEOUT = 25_000L
    }
}
