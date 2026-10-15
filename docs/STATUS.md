# Arbeitsstand — 0.2.0

## Belegt

- Ausgangsrepository enthielt nur README. Implementiert sind CameraX-Preview,
  Front/Hauptkamera, JPEG/MediaStore, FHD-Ziel30 mit Ton, API-geprüftes UHD30
  hinten, öffentlicher Weitwinkel-Kandidat, Fokus/Zoom/Blitz/Torch, Lifecycle,
  Berechtigungen und exportierbare Camera2-/Encoderdiagnose.
- Nutzerbericht Xiaomi 25080RABDG/Android 16 ausgewertet: Installation und
  App-Diagnose belegt. [Geräteprofil](devices/25080RABDG-2026-09-01.md).
- Camera2-Zeitlupenpfad mit festem 120-/240-FPS-Ziel, Hardware-AVC-Gate,
  30-FPS-Wiedergabe ohne Ton, scoped MediaStore und teilbarer Dateiprüfung
  implementiert. Vier konkrete API-Paare vorhanden, Aufnahmebelege noch offen.
- Frontkamera-Fixfokus berücksichtigt. JPEG-Größen und normale/High-Speed-
  FPS werden getrennt dargestellt; HD/FHD/UHD werden im Bericht immer gezeigt.
- Toolchain: JDK 21, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, SDK 35,
  CameraX 1.4.2; Android-Minimum API 29.
- Medienvalidator: `python3 -m unittest discover -s tools -v`, Exit 0,
  18 Tests erfolgreich (15.10.2026). Synthetische Testdaten, keine Geräteaufnahmen.
- `source tools/cloud-env.sh` und `./gradlew --no-daemon testDebugUnitTest
  lintDebug assembleDebug assembleDebugAndroidTest`: Exit 0, BUILD SUCCESSFUL.
  21 Kotlin-Tests, null Fehler/Skips; Lint null Fehler/Warnungen.
  Letzter kompletter Lauf: 52 Sekunden, 76 Tasks, Exit 0.
- Debug-APK (30.609.342 Bytes) und Gerätetest-APK (1.030.634 Bytes) erzeugt;
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
Foto-/Video-/Thermikabnahme: `PENDING DEVICE`. Sechs Hardwaretests kompiliert,
aber hier nicht ausgeführt: zwei Alltagstests und vier Zeitlupentests.
Reguläre 60 FPS, native 200 MP und Ultraweitwinkel sind im gemeldeten
öffentlichen API-Profil blockiert (AE/Timing bzw. UHR/Maximalmap/ID fehlen).
4K30 hinten und Zeitlupe sind API-Kandidaten; erfolgreiche Aufnahmen noch
nicht bestätigt. File-Check-PASS macht keinen Modus `DEVICE_VERIFIED`.
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

SHA-256: `bde05105376a49635c57529d3865402b98350af7b49989af62b56ad8066009da`

`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

SHA-256: `08438e79a23890f99e975a7393e7763a1e11965fd8a1a53aa1bd3e4fd4fdfe55`

Diese Hashes gelten für die lokalen Debug-Artefakte dieses Laufs. Andere
Debug-Schlüssel/Builds können andere Hashes erzeugen. Source-Commit steht neben
der APK in COMMIT.txt; die Binärdateien sind nicht ins Git eingecheckt.
