# Capability-Matrix

Zielmodell: Redmi Note 15 Pro **5G**, nicht 4G. OEM-Angaben stammen aus der
bereitgestellten Spezifikation; noch keine eigene Messung der Geräte-API.

| Kamera / Funktion | Status | API-/Datei-/Gerätenachweis |
|---|---|---|
| Hauptkamera 200 MP, OIS, Pixel-Binning | OEM_ADVERTISED | PENDING DEVICE; UHR, maximale Stream-Map und Pixelmodus untersuchen |
| Ultraweitwinkel 8 MP | OEM_ADVERTISED | PENDING DEVICE; öffentliche ID/Physical-ID untersuchen |
| Front 20 MP | OEM_ADVERTISED | PENDING DEVICE |
| Hinten 1080p30/60, 4K30 | OEM_ADVERTISED | PENDING DEVICE; Encoder, tatsächliche Datei, PTS und Thermik |
| Front 1080p30/60 | OEM_ADVERTISED | PENDING DEVICE; getrennt von hinten prüfen |
| Ultraweitwinkel 1080p30 | OEM_ADVERTISED | PENDING DEVICE |
| Hinten 720p120/240, 1080p120 | OEM_ADVERTISED | PENDING DEVICE; gesonderte High-Speed-Session erforderlich |
| Front 720p120 | OEM_ADVERTISED | PENDING DEVICE; gesonderte High-Speed-Session erforderlich |
| HDR/Nacht/Portrait/Pro/ShootSteady | OEM_ADVERTISED | Machbarkeitsprüfung offen; kein Drittanbieter-Zugriff zugesagt |

Die Diagnoseoberfläche liefert zunächst Fähigkeiten. Sie hebt diese Tabelle
nicht automatisch auf `DEVICE_VERIFIED`. Pro Firmware Kamera-ID, Modus, Datum,
Commit, Diagnoseexport, Originaldatei und Prüfbericht zusammen aufbewahren.
1×/2×/4× bedeuten keine zusätzlichen Teleobjektive.

Quellen aus dem Anhang:

- https://www.mi.com/de/product/redmi-note-15-pro-5g/specs/
- https://www.mi.com/de/support/faq/details/KA-1000115/
- https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics
- https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#SENSOR_PIXEL_MODE
