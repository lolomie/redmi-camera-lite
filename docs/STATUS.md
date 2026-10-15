# Arbeitsstand

## Belegt

- Ausgangsrepository enthielt nur README. Implementiert sind CameraX-Preview,
  Front/Hauptkamera, JPEG/MediaStore, FHD-Ziel30 mit Ton, API-geprüftes UHD30
  hinten, öffentlicher Weitwinkel-Kandidat, Fokus/Zoom/Blitz/Torch, Lifecycle,
  Berechtigungen und exportierbare Camera2-/Encoderdiagnose.
- Toolchain: JDK 21, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, SDK 35,
  CameraX 1.4.2; Android-Minimum API 29.
- Medienvalidator: `python3 -m unittest discover -s tools -v`, Exit 0,
  11 Tests erfolgreich (15.10.2026). Synthetische Testdaten, keine Geräteaufnahmen.
- `source tools/cloud-env.sh` und `./gradlew --no-daemon testDebugUnitTest
  lintDebug assembleDebug assembleDebugAndroidTest`: Exit 0, BUILD SUCCESSFUL.
  Sechs Kotlin-Tests, null Fehler/Skips; Lint null Fehler/Warnungen.
- Debug-APK (30.785.238 Bytes) und Gerätetest-APK (1.005.177 Bytes) erzeugt;
  beide APK-Signaturen mit `apksigner verify --verbose` erfolgreich geprüft.
  Berechtigungen der APK: Kamera, Mikrofon und AndroidX-interne Signature-
  Berechtigung; keine INTERNET- oder Speicherleseberechtigung.
- Setup-Skript wiederholt: Exit 0, SDK/Gradle/JDK wiederverwendet.
- FFmpeg→ffprobe→Validator mit synthetischen 1080p30-/60-Dateien erfolgreich;
  30 als 60 wird korrekt abgewiesen. [TEST_EVIDENCE.md](TEST_EVIDENCE.md).
- CI-Datei enthält Unit-Test/Lint/APK-Ziele, SHA-256 und Commit-Provenienz.
  Action-Pins setup-java v4.7.1 und upload-artifact v4.6.2 wurden per
  `git ls-remote` gegen die offiziellen GitHub-Repositories verifiziert.

## Offen

Git-Remote-Lesezugriff bestätigt; GitHub-API-Aufruf antwortet `Forbidden`.
Remote-CI-/Push-Stand steht in [HANDOFF.md](HANDOFF.md); eine Workflow-Datei allein
beweist keinen ausgeführten GitHub-Lauf.
Alle physischen Kamera-/Audio-/Thermiktests: `PENDING DEVICE`.
60 FPS, 200-MP-Capture und High-Speed-Aufnahmepfade sind noch nicht freigeschaltet
oder implementiert. Die Diagnose für diese Modi ist implementiert; Auswahl der
konkreten Camera2-Pfade benötigt den Geräteexport, danach Implementierung und
Aufnahmeabnahme. Weitwinkel und 4K sind API-bedingte, unbestätigte Kandidaten.
OEM-Angaben stehen in CAPABILITIES, optionale Modi in FEASIBILITY.
Die Cloud besitzt keine durchgereichte USB-Geräteschnittstelle. ADB scheitert
hier zudem an `/home/agent/.android` (schreibgeschützt); Installation/Tests deshalb
von einem Rechner mit angeschlossenem, entsperrtem Redmi gemäß INSTALL ausführen.

## Gates

G0 Repository/Build: bestanden. G1 Preview/Rechte, G2 Foto,
G3 1080p30+Audio, G4 1080p60, G5 Dauerstabilität/Übergabe: Geräteabnahme offen.
Owner-Rollen und Akzeptanzkriterien: [BACKLOG.md](BACKLOG.md).

## Artefakte dieses Laufs

`app/build/outputs/apk/debug/app-debug.apk`

SHA-256: `7f67ae81cbf962fcf7d500c2baf2b46d3c1ec01594f46fd3a7402609ae0409a7`

`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

SHA-256: `046eb7f450cd45ad93c66ed84ff291395885af36935196d79100cac936641e9b`

Diese Hashes gelten für die lokalen Debug-Artefakte dieses Laufs. Andere
Debug-Schlüssel/Builds können andere Hashes erzeugen. Source-Commit steht neben
der APK in COMMIT.txt; die Binärdateien sind nicht ins Git eingecheckt.
