# Capability-Matrix

Zielmodell: Redmi Note 15 Pro **5G**, nicht 4G. Zusätzlich zu den OEM-Angaben
liegt jetzt ein vom Nutzer übermittelter App-Diagnosebericht für Xiaomi
`25080RABDG`, Android 16/API 36, Sicherheitspatch 2026-09-01 vor. Die Cloud
hat das Gerät nicht direkt abgefragt. [Gerätebeleg und Abnahme](devices/25080RABDG-2026-09-01.md).

| Kamera / Funktion | Status | API-/Datei-/Gerätenachweis |
|---|---|---|
| Hauptkamera JPEG | API_EXPOSED | Kamera 0: maximal 4080×3060 = 12,4848 MP; native Sensorgeometrie dadurch nicht bewiesen |
| Native 200 MP | BLOCKED | Keine UHR-Capability, Maximum-Resolution-Map oder SENSOR_PIXEL_MODE; kein öffentlicher 200-MP-Pfad im Bericht |
| Ultraweitwinkel 8 MP / 1080p30 | BLOCKED | Nur öffentliche IDs 0/1, keine Physical-IDs; keine separat erreichbare Ultraweitwinkelkamera |
| Front JPEG | API_EXPOSED | Kamera 1: maximal 2592×1944 = 5,0388 MP; keine Aussage über physische Sensorauflösung |
| Hinten 1080p30 / 4K30 | API_EXPOSED | CameraX FHD/UHD, normale AE-Ranges bis 30; Aufnahme, Ton, PTS und Thermik noch offen |
| Front 1080p30 | API_EXPOSED | CameraX FHD, normale AE-Ranges bis 30; Dateiabnahme offen |
| Normales 1080p60 vorne/hinten | BLOCKED | Kein normaler AE-60-FPS-Pfad gemeldet; Encoder-60-FPS-Fähigkeit reicht nicht |
| Hinten 720p120/240, 1080p120 | API_EXPOSED | Constrained-High-Speed-Konfigurationen; Aufnahme-/Dateiabnahme offen |
| Front 720p120 | API_EXPOSED | Constrained-High-Speed-Konfiguration; Aufnahme-/Dateiabnahme offen |
| Stabilisierung | API_EXPOSED | Hinten OIS OFF/ON; vorne/hinten EIS OFF/ON; wirksame Aufnahmekombination noch offen |
| HDR/Nacht/Portrait/Pro/ShootSteady | OEM_ADVERTISED | Machbarkeitsprüfung offen; kein Drittanbieter-Zugriff zugesagt |

Die Diagnoseoberfläche liefert zunächst Fähigkeiten. Sie hebt diese Tabelle
nicht automatisch auf `DEVICE_VERIFIED`. Pro Firmware Kamera-ID, Modus, Datum,
Commit, Diagnoseexport, Originaldatei und Prüfbericht zusammen aufbewahren.
1×/2×/4× bedeuten keine zusätzlichen Teleobjektive.

`BLOCKED` ist auf die aktuell gemeldeten öffentlichen APIs und diese Firmware
begrenzt, keine Aussage, dass die beworbenen Sensoren physisch fehlen. Keine
versteckten IDs ausprobieren. 120-FPS-High-Speed macht normale 60-FPS-Aufnahme
nicht automatisch verfügbar. Fotos/Videos wurden noch nicht als `CAPTURED`
oder `DEVICE_VERIFIED` bestätigt.

Quellen aus dem Anhang:

- https://www.mi.com/de/product/redmi-note-15-pro-5g/specs/
- https://www.mi.com/de/support/faq/details/KA-1000115/
- https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics
- https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#SENSOR_PIXEL_MODE
