import copy
import unittest
import subprocess
import sys
from pathlib import Path
from validate_media import validate, validate_slow_motion


def fixture(fps=30, seconds=6):
    return {"streams": [
        {"index": 0, "codec_type": "video", "width": 1920, "height": 1080,
         "avg_frame_rate": f"{fps}/1", "duration": str(seconds)},
        {"index": 1, "codec_type": "audio", "duration": str(seconds)}],
        "frames": [{"media_type": "video", "stream_index": 0,
                    "best_effort_timestamp_time": str(i / fps)} for i in range(fps * seconds)]}


class MediaValidationTest(unittest.TestCase):
    def test_valid_30_and_60(self):
        for fps in (30, 60):
            self.assertTrue(validate(fixture(fps), f"1080p{fps}")["passed"])

    def test_nominal_60_with_actual_30_fails(self):
        data = fixture()
        data["streams"][0]["avg_frame_rate"] = "60/1"
        self.assertFalse(validate(data, "1080p60")["checks"]["effective_fps"])

    def test_frame_drop_detected(self):
        data = fixture()
        del data["frames"][50]
        report = validate(data, "1080p30")
        self.assertFalse(report["checks"]["frame_pacing"])
        self.assertEqual(report["measurements"]["estimated_missing_frames"], 1)

    def test_repeated_and_missing_timestamps(self):
        data = fixture()
        data["frames"][1] = copy.deepcopy(data["frames"][0])
        self.assertFalse(validate(data, "1080p30")["checks"]["timestamps_monotonic"])
        del data["frames"][2]["best_effort_timestamp_time"]
        self.assertFalse(validate(data, "1080p30")["checks"]["timestamps_present"])

    def test_resolution_audio_and_duration(self):
        data = fixture()
        data["streams"][0]["width"] = 1280
        data["streams"][1]["duration"] = "2"
        report = validate(data, "1080p30")
        self.assertFalse(report["checks"]["dimensions"])
        self.assertFalse(report["checks"]["audio_duration"])
        self.assertFalse(validate(fixture(seconds=1), "1080p30")["checks"]["minimum_duration"])

    def test_optional_audio_and_no_video(self):
        data = fixture()
        data["streams"].pop()
        self.assertFalse(validate(data, "1080p30")["passed"])
        self.assertTrue(validate(data, "1080p30", require_audio=False)["passed"])
        self.assertFalse(validate({}, "1080p30")["passed"])

    def test_nonfinite_timestamp_rejected(self):
        data = fixture()
        data["frames"][20]["best_effort_timestamp_time"] = "NaN"
        self.assertFalse(validate(data, "1080p30")["passed"])

    def test_short_decoded_sample_cannot_pass_using_stream_duration(self):
        data = fixture()
        data["frames"] = data["frames"][:2]
        report = validate(data, "1080p30")
        self.assertFalse(report["passed"])
        self.assertFalse(report["checks"]["decoded_minimum_duration"])
        self.assertFalse(report["checks"]["decoded_duration_matches_stream"])

    def test_missing_tail_rejected_even_above_minimum_duration(self):
        data = fixture()
        data["frames"] = data["frames"][:-15]
        report = validate(data, "1080p30")
        self.assertTrue(report["checks"]["decoded_minimum_duration"])
        self.assertFalse(report["checks"]["decoded_duration_matches_stream"])
        self.assertFalse(report["passed"])

    def test_decoded_duration_used_when_stream_duration_missing(self):
        data = fixture()
        del data["streams"][0]["duration"]
        self.assertTrue(validate(data, "1080p30")["passed"])

    def test_nonzero_timestamp_origin_is_supported(self):
        data = fixture()
        for frame in data["frames"]:
            frame["best_effort_timestamp_time"] = str(float(frame["best_effort_timestamp_time"]) + 10)
        self.assertTrue(validate(data, "1080p30")["passed"])


class SlowMotionValidationTest(unittest.TestCase):
    @staticmethod
    def slow_fixture(seconds=20, hd=False):
        data = fixture(fps=30, seconds=seconds)
        data["streams"].pop()
        if hd:
            data["streams"][0].update(width=1280, height=720)
        return data

    def test_120_capture_at_30_playback(self):
        report = validate_slow_motion(self.slow_fixture(), "1080p30", 120, 5)
        self.assertTrue(report["passed"])
        self.assertEqual(report["measurements"]["estimated_capture_fps"], 120)
        self.assertAlmostEqual(report["measurements"]["measured_slowdown_ratio"], 4)

    def test_240_capture_at_30_playback_hd(self):
        report = validate_slow_motion(self.slow_fixture(seconds=40, hd=True), "720p30", 240, 5)
        self.assertTrue(report["passed"])
        self.assertEqual(report["measurements"]["estimated_capture_fps"], 240)

    def test_ordinary_30_cannot_be_relabelled_120(self):
        report = validate_slow_motion(self.slow_fixture(seconds=5), "1080p30", 120, 5)
        self.assertFalse(report["passed"])
        self.assertFalse(report["checks"]["estimated_capture_fps"])
        self.assertFalse(report["checks"]["slowdown_ratio"])

    def test_wrong_wall_time_rejected_both_directions(self):
        for wall_time in (2.5, 10):
            with self.subTest(wall_time=wall_time):
                report = validate_slow_motion(self.slow_fixture(), "1080p30", 120, wall_time)
                self.assertFalse(report["passed"])
                self.assertFalse(report["checks"]["estimated_capture_fps"])

    def test_unexpected_audio_rejected(self):
        report = validate_slow_motion(fixture(seconds=20), "1080p30", 120, 5)
        self.assertFalse(report["checks"]["audio_absent"])
        self.assertFalse(report["passed"])

    def test_invalid_parameters(self):
        for mode, fps, wall in (("1080p60", 120, 5), ("1080p30", 60, 5),
                               ("1080p30", 120, 0), ("1080p30", 120, -5),
                               ("1080p30", 120, float("nan")),
                               ("1080p30", 120, float("inf"))):
            with self.subTest(mode=mode, fps=fps, wall=wall):
                with self.assertRaises(ValueError):
                    validate_slow_motion({}, mode, fps, wall)

    def test_cli_invalid_parameter_pairs_and_audio_flag(self):
        script = Path(__file__).with_name("validate_media.py")
        for extra in (("--capture-fps", "120"),
                      ("--capture-seconds", "5"),
                      ("--capture-fps", "120", "--capture-seconds", "5"),
                      ("--capture-fps", "120", "--capture-seconds", "nan", "--no-audio")):
            result = subprocess.run([sys.executable, str(script), "does-not-exist.json",
                                     "--mode", "720p30", *extra], capture_output=True, text=True)
            self.assertEqual(result.returncode, 2)
            self.assertIn("error:", result.stderr)
            self.assertNotIn("Ungültige ffprobe-Datei", result.stderr)


if __name__ == "__main__":
    unittest.main()
