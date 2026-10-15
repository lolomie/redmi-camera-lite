# Product requirements — Redmi Camera Lite (updated 2026-10-15)

## Vision and device
Private, lightweight, robust Kotlin Android camera app for **Xiaomi Redmi Note 15 Pro 5G** (not the 4G model), optimized for dependable front/rear video, native photos and low avoidable overhead. Read `official-device-capabilities.md` for manufacturer-verified camera modes and source URLs. The user's Xiaomi stock camera previously had intermittent black screen/freeze, while third-party apps and CIT tests worked. Do not infer hardware defect or promise superior thermals.

## P0 — stable camera MVP
- Compose interface, lifecycle-aware CameraX preview, camera/microphone permissions, switch front/main rear, focus/zoom, flash/torch when available, capture JPEG to MediaStore, video with audio to MediaStore, error recovery.
- Main and front 1080p30; capability-gated 1080p60 on **both** cameras; rear 4K30 if available to third parties. Verify actual frame timestamps, not only nominal encoder metadata.
- On-device Camera2 diagnostics with exportable sanitized capability matrix; distinguish OEM support from exposed API and tested capture.
- Private GitHub `https://github.com/lolomie/redmi-camera-lite`, CI tests and downloadable debug APK. Repository exists as a user-provided URL but access and contents must be verified; never assume authorization.

## P1 — high-value advanced camera modes
- **200 MP main sensor stills**, only if genuine full-resolution capture is exposed; inspect Android maximum-resolution pixel mode and high-resolution stream map. Show explicit unsupported state otherwise. Preserve standard binned photo mode for everyday use.
- 8 MP ultrawide photo/video (OEM 1080p30) if logical/physical camera accessible.
- Rear 720p120/240 and 1080p120 slow motion, front 720p120 if exposed. Distinguish capture FPS and playback FPS, audio limitations, and high-speed sessions.
- Optional 720p30 modes, timer, grid, horizon level, exposure compensation, focus lock, stabilization where exposed, HEVC option, panorama and time-lapse as feasible.

## P2 — conditional advanced features
- Portrait/blur, HDR, night mode, Pro controls (ISO/shutter/WB/focus), motion tracking, document capture, dynamic photos, filters and other manufacturer app features only after feasibility and UX/performance review. OEM AI/gallery editing is not required and cannot be assumed accessible.

## Constraints
No root, proprietary reverse engineering, hidden/privileged vendor API hacks, cloud analytics, advertisements or unneeded network permissions. Avoid misleading feature toggles. CameraX preferred, Camera2 fallback only with documented technical justification. Keep build versions current and compatible.

## Verification and acceptance
- Compile and run tests in CI; publish debug APK artifact, record commit SHA and checksum.
- Real device: inspect Android/HyperOS build, test permission denials, lifecycle, photos, 1080p30, front/rear 1080p60, rear 4K30, 200 MP, ultrawide, slow motion where exposed. Mark untested `PENDING DEVICE`.
- Validate native 200 MP dimensions and video timestamps; record blocked paths. Benchmark thermal behavior under matched brightness, duration, environment and charging conditions.
- Docs: architecture, capability report, backlog, decisions, status, device tests, release notes and installation instructions.
