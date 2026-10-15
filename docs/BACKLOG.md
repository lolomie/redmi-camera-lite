# Priorisierter Backlog und Abnahme

Ein implementierter Pfad ist erst nach seinem Abnahmetest abgeschlossen.
Geräteprüfungen stehen bis zu dokumentierten Messungen auf `PENDING DEVICE`.

| Prio | Aufgabe | Verantwortliche Rolle | Akzeptanz / Nachweis |
|---|---|---|---|
| P0 | Reproduzierbarer Build | DevOps | JDK/Gradle-Pins; `testDebugUnitTest lintDebug assembleDebug` erfolgreich; APK+SHA-256 |
| P0 | Berechtigungen/Preview | Kotlin/UX/QA | Erststart, Ablehnung und erneute Freigabe; Haupt-/Frontkamera; Rotation; kein schwarzer Dauerzustand |
| P0 | Fotos | Camera Engineer | JPEG in MediaStore; korrektes Bild, Orientierung und Neustart-Persistenz |
| P0 | Video mit Audio | Camera Engineer/QA | Vorne/hinten 1080p30; Datei abspielbar, Ton hörbar, Validator erfolgreich |
| P0 | Fokus/Zoom/Blitz | UX/Camera Engineer | Tap-Fokus und Zoom; nur verfügbare Blitz/Torch-Optionen; klare Fehler |
| P0 | Lifecycle und Datenschutz | Security/QA | Hintergrund/Zurück stoppt Zugriff; keine Netzwerkberechtigung; kein Medienverlust bei Fehler |
| P0 | Exportierbare Diagnose | Camera Engineer | IDs, Größen, FPS, High-Speed, Stabilisierung und Encoder; ohne Geheimnisse |
| P1 | Front/hinten 1080p60 | Camera/Performance | API-Kombination, echte Aufnahme, Framezeiten, mindestens 5 Minuten Stabilität |
| P1 | Hinten 4K30 | Camera/QA | API-Kombination, native 3840×2160-Datei, Framezeiten, Ton und Dauerlauf |
| P1 | Native 200 MP | Camera Engineer | UHR-Capability, Maximum-Resolution-Map/Pixelmodus, Aufnahme-Metadaten und echte Dimensionen; kein Upscaling |
| P1 | 8-MP-Ultraweitwinkel | Camera Engineer | Öffentliche Kamera-/Physical-ID erreichbar, eindeutiger optischer Wechsel, native Aufnahme |
| P1 | Slow Motion | Camera/QA | Öffentliche High-Speed-Session: hinten 720p120/240,1080p120; vorne 720p120; Aufnahme-/Wiedergabe-FPS getrennt |
| P1 | Performance | Performance Engineer | Vergleich bei gleichem Licht, Helligkeit, Laufzeit, Temperatur und Ladezustand |
| P2 | HDR/Nacht/Portrait/Pro/ShootSteady | Architect/UX | Öffentliche API-Machbarkeit und Kosten geprüft; eigene Abnahmekriterien vor Implementierung |
| P2 | Timer/Grid/Horizont/Belichtung/HEVC | Product Owner | Nach P0/P1 einzeln priorisieren; keine ungeprüften OEM-Versprechen |

Aktuelle Resultate: [STATUS.md](STATUS.md). Geräteabnahme: [DEVICE_TESTS.md](DEVICE_TESTS.md).

## Gerätebasierte Priorisierung

Nutzerdiagnose Xiaomi 25080RABDG/API 36 liegt vor (siehe
[Gerätebericht](devices/25080RABDG-2026-09-01.md)). Installation und Diagnose
sind belegt; Foto-/Videoabnahmen bleiben offen. Zuerst 30-FPS-Alltagspfad und
Rückwechsel/Lifecycle sichern. Danach die vier exponierten High-Speed-
Kombinationen separat abnehmen. Front ist Fixfokus; AF-UI dort deaktivieren,
Blitz nur hinten. Native 200 MP, Ultraweitwinkel und normales 60 FPS bleiben
auf diesem API-/Firmware-Stand blockiert; kein Fake-Modus und keine Hidden-ID-
Suche. Encodergrenzen dürfen diese Sperre nicht aufheben.
