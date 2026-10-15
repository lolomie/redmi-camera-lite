# Übergabe — 15.10.2026

Der installierbare Entwicklungsstand wurde lokal gebaut und geprüft. Es ist
noch keine vollständig am Redmi abgenommene Release-App.

## GitHub und Artefakte

- Source-/Build-Stand: `37e3eb1`, Branch `codex/redmi-camera-lite-mvp`.
- Push auf `github.com/lolomie/redmi-camera-lite` war erfolgreich. Kein Merge
  nach main und kein Release durchgeführt.
- Workflow für Build/Unit-Tests/Lint/APK ist im gepushten Branch enthalten.
  GitHub-API antwortete `Forbidden`; CI-Laufstatus und private Repo-Metadaten
  wurden deshalb nicht bestätigt. Kein Pull Request angelegt.
- Lokale APK, Test-APK und Prüfsummen: [STATUS.md](STATUS.md). Der Code wurde
  seit diesem Build nicht verändert; diese Übergabe ergänzt nur Dokumentation.
- Lokale Buildprotokolle: `/tmp/redmi-build.log`, Setup-Wiederholung:
  `/tmp/redmi-setup-repeat.log`. Langfristige Belege stehen in STATUS und
  TEST_EVIDENCE; temporäre Cloud-Dateien sind keine dauerhafte Ablage.

## Wiederverwendbare Umgebung

`install_script` und `start_skill` wurden erfolgreich als Umgebungsentwurf
gespeichert. SDK, JDK, Gradle, Caches und Debug-Key liegen außerhalb des
Checkouts. Keine Serverprozesse erforderlich. Der Entwurf ergänzt
`api.github.com` zur Netzwerkfreigabe und bewahrt die Paketmanager-Presets.
Speichern des Entwurfs führt keine Skripte aus und veröffentlicht keinen
Snapshot. In den Umgebungseinstellungen prüfen/speichern und veröffentlichen;
Wiederherstellung in einer neuen Aufgabe wurde nicht getestet.

## Konkreter nächster Nachweis

1. Debug-APK auf dem Redmi Note 15 Pro **5G** installieren, Foto/Video mit Ton,
   Rechte und Rückkehr aus dem Hintergrund prüfen.
2. Diagnose in der App exportieren; Modell/Android/HyperOS bestätigen.
3. Die zwei automatisierten Hardwaretests und die manuellen Tests gemäß
   INSTALL und DEVICE_TESTS ausführen; Originaldateien samt Berichten behalten.
4. Auf Basis der exponierten Kamera-/Encoderkombinationen 60-FPS-, UHR- und
   High-Speed-Aufnahmepfade implementieren und gesondert testen. Diese Pfade
   sind aktuell Diagnose-/Machbarkeitsarbeit, keine fertigen Aufnahmeoptionen.

Die Cloud hat kein nutzbares durchgereichtes Redmi. ADB konnte sein
System-Home-Verzeichnis nicht beschreiben. Ohne ein reales Gerät können
Sensorauflösung, Audioqualität, Dateitimestamps und Treiberstabilität nicht
belegt werden. Der vollständige Produktumfang ist damit noch nicht abgenommen.
