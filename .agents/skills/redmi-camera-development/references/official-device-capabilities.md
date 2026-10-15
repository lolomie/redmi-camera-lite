# Redmi Note 15 Pro 5G — verified manufacturer capability inventory

Research date: 2026-10-15. Scope: **Redmi Note 15 Pro 5G**, not Redmi Note 15 Pro (4G). Do not substitute specs from the similarly named 4G model (32 MP selfie, different video options).

## Official Xiaomi camera hardware
- Rear main: 200 MP Samsung HPE sensor, 1/1.4 inch, f/1.7, OIS, 7P optics, 16-in-1 pixel binning (manufacturer lists 0.56 μm native and 2.24 μm binned effective pixels).
- Rear ultrawide: 8 MP OmniVision OV08F, f/2.2; FAQ identifies 1/4 inch sensor.
- Front: 20 MP, f/2.2, 1/4 inch, 4P optics.
- Manufacturer marketing: 1x, 1.2x, 1.5x, 2x, 4x equivalent views from the main camera. Treat these as crop/processing options, not evidence of physical telephoto lenses.

## Official Xiaomi camera app recording modes
| Camera | Video | Slow motion |
| --- | --- | --- |
| Main rear | 720p30, 1080p30/60, 4K30 | 720p120/240, 1080p120 |
| Front | 720p30, 1080p30/60 | 720p120 |
| Ultrawide | 720p30, 1080p30 | Not specified |

## Official Xiaomi camera software features
Rear: motion-tracking focus, motion capture, portrait, panorama, Pro, time-lapse, automatic night mode, document mode, slow motion, blur, beauty, video filters, video beautification, ShootSteady, dynamic photos, timer, level, HDR, watermark, audio shutter; the Xiaomi FAQ also lists a timeline feature. Front: portrait, time-lapse, teleprompter, filters, timer, dynamic photos, blur, audio shutter.

Other OEM promotional capabilities: AI erase, reflection removal, image expansion, sky, bokeh, cutouts, image enhancement, beautification, AI film. These may be proprietary, gallery-based or network-based and are NOT requirements for the private camera MVP.

## Strict distinction: OEM feature versus third-party app access
All above modes are *manufacturer advertised*, not verified for CameraX/Camera2 third-party access. A feature becomes app-supported only after capability discovery, successful capture and file/device verification. Do not upscale images and label them native 200 MP. Do not use unsupported Camera2 modes, root, vendor libraries, or privileged APIs.

## Camera2 capability investigation checklist
1. Log Android version, HyperOS version, manufacturer, model, build fingerprint (sanitize report before sharing).
2. Enumerate logical/physical camera IDs, lens facing, camera hardware level, physical camera availability, available capabilities and output sizes.
3. Inspect `REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR`, `SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION`, `SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION`, `CaptureRequest.SENSOR_PIXEL_MODE` and supported JPEG/HEIC outputs. Handle missing keys and vendor-restricted paths gracefully.
4. For each video stream inspect supported sizes, AE FPS ranges, min frame duration, MediaCodec encoder capabilities, CameraX quality/profile availability and feasible surface combinations. Inspect high-speed video configurations separately; 120/240 fps is not normal 60 fps recording.
5. Inspect stabilization modes (OIS/EIS), focus, exposure, flash, zoom and ultrawide availability independently. Hardware OIS does not guarantee controllable OIS from third-party apps.
6. Capture sample media; validate actual pixel dimensions, duration, presentation timestamps, effective FPS, dropped frames, codec, and audio. For 200 MP, validate native pixel count and metadata, not just output filename.
7. Produce a per-mode matrix with `OEM_ADVERTISED`, `API_EXPOSED`, `CAPTURED`, `DEVICE_VERIFIED`, or `BLOCKED` and evidence references.

## Authoritative references
- Xiaomi Germany, exact 5G model specs: https://www.mi.com/de/product/redmi-note-15-pro-5g/specs/
- Xiaomi Germany, exact 5G model FAQ (camera features, slow motion, sensor vendors): https://www.mi.com/de/support/faq/details/KA-1000115/
- Xiaomi Germany product page (200 MP and zoom positioning): https://www.mi.com/de/product/redmi-note-15-pro-5g/
- Android CameraCharacteristics: https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics
- Android CaptureRequest SENSOR_PIXEL_MODE: https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#SENSOR_PIXEL_MODE

Note: Xiaomi FAQ may describe OEM modes available in stock software; exact availability can vary with region and firmware.
