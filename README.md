# Redmi Camera Lite

Private Kotlin-/Compose-Kamera-App für das Redmi Note 15 Pro 5G und öffentliche
Android-Kamera-APIs. Android 10/API 29 oder neuer. Keine Werbung, Telemetrie,
Netzwerkberechtigung oder Root-Abhängigkeit.

Implementiert: CameraX-Vorschau, Haupt-/Frontkamera, JPEG-Fotos und Video mit Ton
über MediaStore, Fokus, Zoom, Blitz/Dauerlicht nach Verfügbarkeit, Lifecycle-
Freigabe und exportierbare Camera2-/Encoder-Diagnose. Video startet mit dem Ziel
1080p30; hinten ist 4K30 bei gemeldeter CameraX-Unterstützung auswählbar.
Eine weiterwinklige separat erreichbare Rückkamera wird nur als unbestätigter
API-Kandidat angeboten.

OEM-Angaben zu 200 MP, 60 FPS und Zeitlupe sind keine Zusage für Drittanbieter-
Zugriff. Diagnosewerte sind keine bestandenen Aufnahmetests. Der aktuelle
Nachweisstand steht in [docs/STATUS.md](docs/STATUS.md).

## Bauen

Android Studio oder JDK 21 mit Android SDK 35/Build Tools 35.0.0:

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
python3 -m unittest discover -s tools -v
```

In dieser Cloud-Umgebung:

```sh
bash tools/setup-android.sh
source tools/cloud-env.sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

Die Debug-APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`.
Die Cloud-Hilfe legt SDK und Caches außerhalb des Checkouts ab und verwendet
den vorhandenen Netzwerkproxy einschließlich Zertifikatsprüfung.

## Abnahme und Übergabe

- [Installation und Artefakte](docs/INSTALL.md)
- [Stand und Testnachweise](docs/STATUS.md)
- [Gerätetests und Medienprüfung](docs/DEVICE_TESTS.md)
- [Backlog mit Akzeptanzkriterien](docs/BACKLOG.md)
- [Architektur](docs/ARCHITECTURE.md), [Entscheidungen](docs/DECISIONS.md)
- [API-/OEM-Grenzen](docs/CAPABILITIES.md), [Datenschutz](docs/SECURITY.md)

Der GitHub-Workflow baut und prüft die App und stellt APK, Prüfsumme und
Commit-Zuordnung als kurzlebiges CI-Artefakt bereit, sobald er im Repository läuft.
