# Entscheidungen

1. **CameraX zuerst.** Lifecycle, MediaStore und Recorder bilden den stabilen
   Alltagspfad. Camera2 wird für öffentliche Capability-Diagnostik verwendet.
2. **Evidenz vor Modusfreigabe.** `OEM_ADVERTISED` bedeutet Herstellerangabe,
   `API_EXPOSED` eine konkrete Geräte-API-Messung, `CAPTURED` eine gespeicherte
   Aufnahme, `DEVICE_VERIFIED` die bestandene Abnahme. `BLOCKED` beschreibt den
   belegten fehlenden Zugriff. Ohne Gerät wird kein höherer Status behauptet.
3. **Kein Fake-200-MP-Modus.** Bilddimensionen allein können Upscaling nicht
   ausschließen. Sensor-Pixelmodus, Stream-Konfiguration und Capture-Metadaten
   müssen zusammenpassen; keine Freigabe aus bloßer JPEG-Größe.
4. **Keine Slow-Motion-Simulation.** 120/240-FPS-API-Ranges sind kein Nachweis
   einer High-Speed-Session. Wiedergabe-FPS sind nicht Aufnahme-FPS.
5. **Private lokale Medien.** Kein INTERNET, Tracking, Werbung oder Upload.
   Diagnoseexport erfolgt nur auf Nutzeraktion; Build-Fingerprint und persönliche
   Bild-/Tondaten vor Weitergabe prüfen.
6. **Debug-Artefakt klar kennzeichnen.** Debug-Signatur ist für Entwicklung,
   nicht für öffentliche Distribution; Release-Key bleibt außerhalb des Git.
7. **Automatisierte CI ohne Schreibrechte.** Actions sind per SHA gepinnt,
   GitHub-Token erhält nur `contents: read`; kein automatischer Push/Release.

Quelle der OEM-Anforderungen: mitgelieferte Spezifikation vom 15.10.2026.
Die darin genannten Xiaomi-Quellen sind in [CAPABILITIES.md](CAPABILITIES.md)
verlinkt; öffentliche API-Verfügbarkeit wurde daraus ausdrücklich nicht abgeleitet.

8. **Explizites CameraX-Interop-Opt-in.** Die separat exponierte Weitwinkel-
   Kameraauswahl verwendet öffentliche, in CameraX 1.4.2 noch experimentell
   markierte Camera2-Interop-/CameraInfo-APIs. Opt-in steht am Controller;
   Lint bleibt aktiv. Keine privaten Vendor-IDs oder Reflection.
9. **Rotation ohne Controller-Neustart.** Die Activity behandelt Orientierung/
   Bildschirmgröße selbst; Compose behält dieselbe PreviewView. Laufende Videos
   behalten ihre Startorientierung. Prozessende ist keine fortsetzbare Aufnahme.
10. **Camera2 nur für konkret exponierte Zeitlupe.** Der Nutzerbericht enthält
    vier feste High-Speed-Kombinationen. Daher eigener Camera2-Pfad statt
    gewöhnlicher CameraX-60-FPS-Einstellung. 120/240-Zielaufnahme und 30-FPS-
    Wiedergabe sind getrennt, Audio bewusst deaktiviert. Eine High-Speed-
    Sitzung macht keinen gewöhnlichen 60-FPS-Modus verfügbar.
11. **Firmware-begrenzte Einschränkungen.** Der Bericht exponiert keinen
    UHR-Pfad und keine zusätzliche Ultraweitwinkel-ID. Diese Aufgaben sind
    mit konkretem API-Grund blockiert; kein Root-/Vendor-Workaround. Größere
    OEM-Sensoren werden nicht aus kleineren JPEG-Ausgaben als fehlend erklärt.
