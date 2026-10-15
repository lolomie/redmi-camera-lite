#!/usr/bin/env python3
"""Validate ffprobe JSON without external Python dependencies. Not native-sensor proof."""
import argparse
import json
import math
from fractions import Fraction
from pathlib import Path


MODES = {
    "720p30": (1280, 720, 30),
    "1080p30": (1920, 1080, 30), "1080p60": (1920, 1080, 60),
    "4k30": (3840, 2160, 30), "720p120": (1280, 720, 120),
    "720p240": (1280, 720, 240), "1080p120": (1920, 1080, 120),
}


def number(value):
    try:
        result = float(Fraction(str(value)))
        return result if math.isfinite(result) else None
    except (ValueError, TypeError, ZeroDivisionError):
        return None


def validate(data, mode, require_audio=True, min_duration=5.0, tolerance=0.05):
    width, height, fps = MODES[mode]
    checks = {}
    streams = data.get("streams", [])
    videos = [s for s in streams if s.get("codec_type") == "video"]
    if not videos:
        return {"schema_version": 1, "mode": mode, "passed": False,
                "checks": {"video_stream": False}, "limitations": ["Kein Videostream"]}
    stream = videos[0]
    index = stream.get("index", 0)
    frames = [f for f in data.get("frames", [])
              if f.get("media_type") == "video" and f.get("stream_index", index) == index]
    timestamps = [number(f.get("best_effort_timestamp_time", f.get("pts_time"))) for f in frames]
    valid = [t for t in timestamps if t is not None]
    gaps = [b - a for a, b in zip(valid, valid[1:])]
    span = valid[-1] - valid[0] if len(valid) > 1 else 0
    effective_fps = (len(valid) - 1) / span if span > 0 else None
    decoded_duration = span + 1 / fps if len(valid) > 1 else None
    video_duration = number(stream.get("duration"))
    if video_duration is None:
        video_duration = decoded_duration
    checks["dimensions"] = sorted((stream.get("width", 0), stream.get("height", 0))) == sorted((width, height))
    checks["timestamps_present"] = len(valid) == len(frames) and len(valid) > 1
    checks["timestamps_monotonic"] = bool(gaps) and all(g > 0 for g in gaps)
    checks["effective_fps"] = effective_fps is not None and abs(effective_fps - fps) <= fps * tolerance
    checks["frame_pacing"] = bool(gaps) and all(0 < g <= 1.5 / fps for g in gaps)
    checks["minimum_duration"] = video_duration is not None and video_duration >= min_duration
    checks["decoded_minimum_duration"] = decoded_duration is not None and decoded_duration >= min_duration
    checks["decoded_duration_matches_stream"] = (
        decoded_duration is not None and video_duration is not None and
        abs(decoded_duration - video_duration) <= max(2 / fps, video_duration * 0.02))
    audio_streams = [s for s in streams if s.get("codec_type") == "audio"]
    audio_duration = number(audio_streams[0].get("duration")) if audio_streams else None
    checks["audio_present"] = bool(audio_streams) if require_audio else True
    checks["audio_duration"] = (not require_audio or
        (audio_duration is not None and video_duration is not None and
         abs(audio_duration - video_duration) <= max(0.25, video_duration * 0.02)))
    return {"schema_version": 1, "mode": mode, "passed": all(checks.values()), "checks": checks,
            "measurements": {"dimensions": [stream.get("width"), stream.get("height")],
                "codec": stream.get("codec_name"), "nominal_fps": stream.get("avg_frame_rate"),
                "decoded_frames": len(frames), "effective_fps": effective_fps,
                "maximum_frame_gap_seconds": max(gaps) if gaps else None,
                "estimated_missing_frames": sum(max(0, round(g * fps) - 1) for g in gaps),
                "video_duration_seconds": video_duration, "decoded_duration_seconds": decoded_duration,
                "audio_duration_seconds": audio_duration},
            "criteria": {"target_fps": fps, "fps_tolerance_fraction": tolerance,
                "maximum_gap_frames": 1.5, "minimum_duration_seconds": min_duration,
                "audio_required": require_audio},
            "limitations": ["Dateianalyse beweist weder Aufnahme-FPS eines Zeitlupenexports noch Sensor-Nativauflösung.",
                "Thermik, Bildinhalt, Toninhalt und Kamera-API-Kombination separat am Gerät prüfen."]}


def validate_slow_motion(data, mode, capture_fps, capture_seconds):
    """Compare playback against operator-measured wall time; not unique-frame proof."""
    wall_seconds = number(capture_seconds)
    if mode not in ("720p30", "1080p30"):
        raise ValueError("Zeitlupe benötigt --mode 720p30 oder 1080p30")
    if capture_fps not in (120, 240):
        raise ValueError("Aufnahme-FPS müssen 120 oder 240 sein")
    if wall_seconds is None or wall_seconds <= 0:
        raise ValueError("Reale Aufnahmezeit muss positiv und endlich sein")
    factor = capture_fps / 30
    report = validate(data, mode, require_audio=False,
                      min_duration=wall_seconds * factor * 0.9)
    checks = report["checks"]
    measures = report.setdefault("measurements", {})
    playback_seconds = measures.get("decoded_duration_seconds")
    estimated_capture_fps = measures.get("decoded_frames", 0) / wall_seconds
    measured_factor = playback_seconds / wall_seconds if playback_seconds is not None else None
    checks["audio_absent"] = not any(s.get("codec_type") == "audio" for s in data.get("streams", []))
    checks["estimated_capture_fps"] = abs(estimated_capture_fps - capture_fps) <= capture_fps * 0.1 + 1e-9
    checks["slowdown_ratio"] = measured_factor is not None and abs(measured_factor - factor) <= factor * 0.1 + 1e-9
    measures.update({"operator_capture_seconds": wall_seconds,
                     "estimated_capture_fps": estimated_capture_fps,
                     "measured_slowdown_ratio": measured_factor})
    report.setdefault("criteria", {}).update({"capture_fps_target": capture_fps,
        "expected_slowdown_ratio": factor, "capture_tolerance_fraction": 0.1,
        "audio_must_be_absent": True})
    report["limitations"].extend([
        "Reale Aufnahmezeit ist eine externe Operatorangabe, keine Messung aus der Datei.",
        "Geschätzte Capture-FPS beweisen keine einzigartigen nativen Sensorframes; Duplikate/Interpolation und Session-Metadaten separat prüfen."])
    report["passed"] = all(checks.values())
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("probe", type=Path)
    parser.add_argument("--mode", choices=MODES, required=True)
    parser.add_argument("--no-audio", action="store_true", help="Für High-Speed-Dateien ohne Ton")
    parser.add_argument("--min-duration", type=float, default=5.0)
    parser.add_argument("--capture-fps", type=int, choices=(120, 240),
                        help="Für Zeitlupe: Ziel-Aufnahme-FPS bei 30-FPS-Wiedergabe")
    parser.add_argument("--capture-seconds", type=float,
                        help="Extern gemessene reale Aufnahmezeit; zusammen mit --capture-fps")
    args = parser.parse_args()
    if args.min_duration <= 0 or not math.isfinite(args.min_duration):
        parser.error("--min-duration muss positiv und endlich sein")
    slow_motion = args.capture_fps is not None or args.capture_seconds is not None
    if slow_motion:
        if args.capture_fps is None or args.capture_seconds is None:
            parser.error("--capture-fps und --capture-seconds gemeinsam angeben")
        if args.mode not in ("720p30", "1080p30") or not args.no_audio:
            parser.error("Zeitlupe benötigt 720p30/1080p30 und --no-audio; Tonspuren werden abgewiesen")
        if args.capture_seconds <= 0 or not math.isfinite(args.capture_seconds):
            parser.error("--capture-seconds muss positiv und endlich sein")
    try:
        data = json.loads(args.probe.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            raise ValueError("JSON-Wurzel muss ein Objekt sein")
        report = (validate_slow_motion(data, args.mode, args.capture_fps, args.capture_seconds)
                  if slow_motion else validate(data, args.mode, not args.no_audio, args.min_duration))
    except (OSError, ValueError, TypeError, AttributeError) as error:
        parser.exit(2, f"Ungültige ffprobe-Datei: {error}\n")
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True, allow_nan=False))
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
