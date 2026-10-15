# Entwicklungsversion 0.1.0

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
