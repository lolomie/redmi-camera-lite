# Übergabe — 15.10.2026

Der installierbare Entwicklungsstand wurde lokal gebaut und geprüft. Es ist
noch keine vollständig am Redmi abgenommene Release-App.

## GitHub und Artefakte

- Version 0.2.0, Branch `codex/redmi-camera-lite-mvp`; Source-Commit im
  lokal erzeugten `app/build/outputs/apk/debug/COMMIT.txt`.
- Push auf `github.com/lolomie/redmi-camera-lite` war erfolgreich. Kein Merge
  nach main und kein Release durchgeführt.
- Workflow für Build/Unit-Tests/Lint/APK ist im gepushten Branch enthalten.
  GitHub-API antwortete `Forbidden`; CI-Laufstatus und private Repo-Metadaten
  wurden deshalb nicht bestätigt. Kein Pull Request angelegt.
- Lokale APK, Test-APK und Prüfsummen: [STATUS.md](STATUS.md). Gerätetests
  wurden kompiliert; kein erfolgreicher Aufnahmelauf wird daraus abgeleitet.
- Lokale Buildprotokolle: `/tmp/redmi-build-v02.log`, Setup-Wiederholung:
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

1. Debug-APK 0.2.0 auf dem Redmi Note 15 Pro **5G** als Update installieren, Foto/Video mit Ton,
   Rechte und Rückkehr aus dem Hintergrund prüfen.
2. Der Nutzerdiagnosebericht liegt vor: Xiaomi 25080RABDG/API 36. Das
   zugehörige Geräteprofil und seine API-Grenzen stehen unter docs/devices.
3. Die sechs automatisierten Hardwaretests und die manuellen Tests gemäß
   INSTALL und DEVICE_TESTS ausführen; Originaldateien samt Berichten behalten.
4. Zuerst Zeitlupe hinten HD120 fünf reale Sekunden testen und den
   Dateiprüfbericht teilen. Dann die übrigen drei exponierten Paare testen.
   Erwartet: etwa 20/40 Sekunden Wiedergabe bei 120/240-FPS-Zielaufnahme.
   High-Speed-Pfad ist implementiert, aber die Gerätedateien sind unbestätigt.
5. Reguläre 60 FPS, native 200 MP und Ultraweitwinkel bleiben auf dem
   gelieferten öffentlichen API-Profil blockiert; keine versteckten IDs,
   Vendor-Bibliotheken oder hochskalierten Ersatzmodi.

Die Cloud hat kein nutzbares durchgereichtes Redmi. ADB konnte sein
System-Home-Verzeichnis nicht beschreiben. Ohne ein reales Gerät können
Sensorauflösung, Audioqualität, Dateitimestamps und Treiberstabilität nicht
belegt werden. Der vollständige Produktumfang ist damit noch nicht abgenommen.
