# Entwicklungsversion 0.2.0

- Nutzerdiagnose für Xiaomi 25080RABDG/API 36 ausgewertet: getrennte
  normale AE-FPS, High-Speed-Konfigurationen, Encodergrenzen und JPEG-Ausgaben.
- Camera2-Zeitlupentest mit ausschließlich exakt exponierten 120-/240-FPS-
  Kombinationen, Hardware-AVC-Gate, 30-FPS-Wiedergabe und ohne Mikrofon.
- Scoped-MediaStore-Aufnahme, kontrollierter CameraX↔Camera2-Wechsel,
  Lifecycle-Freigabe und Dateiprüfung samt teilbarem Messbericht.
- Reguläre 60 FPS bleiben gesperrt, wenn gewöhnliche AE-/Stream-Fähigkeiten
  fehlen. UHR-/200-MP- und zusätzliche Objektivfähigkeit werden nicht erfunden.
- Frontkamera-Fixfokus berücksichtigt; Tap kann nur exponierte AE/AWB messen.
- Vier zusätzliche Instrumentationstests für echte Zeitlupen-Dateien vorbereitet.
  Noch kein Gerätelauf dieser Tests und keine bestätigte High-Speed-Aufnahme.

## Entwicklungsversion 0.1.0

Erster nativer Kotlin-/Compose-/CameraX-Aufbau mit Geräte-Diagnose,
lokaler Medienspeicherung und reproduzierbarem Debug-Build. Der tatsächliche
Build-/Teststand steht in [STATUS.md](STATUS.md).

Kein produktiver Release veröffentlicht. Debug-APK ist für Entwicklung und
Geräteabnahme bestimmt. Hardwarefunktionen sind bis zur Geräteabnahme nicht
zertifiziert; insbesondere 200 MP, 60 FPS, 4K, Ultraweitwinkel und Slow Motion
werden nicht aus OEM-Werbung als funktionierende App-Modi abgeleitet.

## Freigabecheckliste

- Unit-Tests, Lint und assembleDebug mit Exit-Code dokumentiert.
- APK-Hash, Commit, Toolchain und Debug-Signatur vermerkt.
- CI-Lauf und Artefakt prüfen, falls auf GitHub ausgeführt.
- Geräteabnahme gemäß DEVICE_TESTS einschließlich Rechte und Lifecycle.
- Native-200-MP-/FPS-Belege oder klare Einschränkung pro Modus.
- Datenschutz und Log-/Exportprüfung; keine Secrets im Repository.
- Produktionssignierung, Store-Veröffentlichung und externe Releases separat
  vorbereiten; aktuell nicht erfolgt.
