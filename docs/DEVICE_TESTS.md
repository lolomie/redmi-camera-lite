# Geräteabnahme — PENDING DEVICE

Noch keine Kameraaufnahme, Installation, Temperaturmessung oder Firmware-Abnahme
am Redmi Note 15 Pro 5G belegt. Cloud-Builds ersetzen diese Tests nicht.

## Protokollkopf

Für jeden Lauf: Datum, Commit/APK-SHA-256, Modell, Android-/HyperOS-Version,
Firmware, Kamera-ID, Modus, Licht, Umgebungstemperatur, Displayhelligkeit,
Ladezustand und Aufnahmedauer dokumentieren. Build-Fingerprint vor Teilen
prüfen. Originalmedien nicht automatisch veröffentlichen.

## Pflichtablauf

1. APK gemäß [INSTALL.md](INSTALL.md) installieren; Modell/5G-Variante bestätigen.
2. Kamera-/Mikrofonrechte zuerst verweigern, dann erteilen; Neustart und
   Systemeinstellungen prüfen. Foto darf ohne Mikrofonrecht funktionieren.
3. Front/hinten Preview, Rotation, Tap-Fokus, Zoom, verfügbare Blitz/Torch-Modi;
   Hintergrund, Sperrbildschirm, Zurück und erneuter Start mindestens zehnmal.
4. Je Kamera zehn Fotos; Galerie, Orientierung, Dimensionen und Persistenz nach
   Neustart prüfen. Aufnahmefehler und vollen Speicher prüfen.
5. Front/hinten 1080p30 je 30 Sekunden mit gesprochenem Zähltext aufnehmen.
   Sicht-/Hörprüfung, A/V-Synchronität und ffprobe-Validator; danach Dauerlauf
   mindestens fünf Minuten bei dokumentierten Bedingungen.
6. Diagnose exportieren. Nur exponierte fortgeschrittene Kombinationen
   separat prüfen: 1080p60 vorne/hinten, 4K30 hinten, Ultraweitwinkel.
7. 200 MP erst mit UHR-Capability, maximaler Stream-Konfiguration und gesetztem
   Maximum-Resolution-Pixelmodus untersuchen; Originaldatei samt Capture-
   Metadaten sichern. Dimensionen ohne Sensorbeleg sind kein Native-Nachweis.
8. Slow Motion benötigt High-Speed-Session, unterstützte Größen/FPS-Kombination
   und bestätigte Aufnahme-FPS. Export mit 30 Wiedergabe-FPS beweist keine
   120/240 Aufnahme-FPS. Audio kann bei High-Speed fehlen.
9. Xiaomi-Kamera und diese App unter identischen Bedingungen vergleichen.
   Ohne Messwerte keine Behauptung geringerer Wärme oder Akkulast.

## Reproduzierbare Videoanalyse

`ffprobe` stammt aus einer vertrauenswürdigen FFmpeg-Installation. JSON immer
aus der unbeschnittenen Originaldatei erzeugen:

```sh
ffprobe -v error -show_streams -show_frames -of json sample.mp4 > sample.probe.json
python3 tools/validate_media.py sample.probe.json --mode 1080p30
python3 tools/validate_media.py sample60.probe.json --mode 1080p60 --min-duration 300
```

Exit 0: alle Dateikriterien bestanden; 1: mindestens eines verfehlt; 2: ungültige
Eingabe. Bericht prüft Dimensionen, monotone PTS, effektive FPS ±5 %, maximale
Frame-Lücke ≤1,5 Ziel-Frames und Audio-/Videolänge (max. 250 ms oder 2 %).
Frameverlust ist eine PTS-basierte Schätzung. Nominale FPS allein bestehen
nicht. `--no-audio` nur für bewusst tonlose High-Speed-Tests. Aufgenommene
Hochgeschwindigkeitssequenzen mit verlangsamter Wiedergabe brauchen zusätzlich
Capture-Metadaten; der Validator bewertet die gespeicherte Wiedergabezeitachse.
Decodierte Zeitspanne und deklarierte Dauer müssen zusammenpassen; wenige
Frames mit einer gefälschten langen Dauerangabe bestehen nicht.

Die zwei vorbereiteten `CameraDeviceTest`-Instrumentationstests prüfen echte
FHD30-/JPEG-Samples vorne/hinten, Audiotracks und hinten Rotation. Sie sind
kompiliert, aber in diesem Cloud-Lauf nicht ausgeführt. Anleitung: INSTALL.

| Test | Ergebnis | Beleg |
|---|---|---|
| Installation/Preview/Rechte/Lifecycle | PENDING DEVICE | — |
| JPEG vorne/hinten | PENDING DEVICE | — |
| Video 1080p30 mit Audio vorne/hinten | PENDING DEVICE | — |
| 1080p60 / 4K30 | PENDING DEVICE | — |
| Native 200 MP / Ultraweitwinkel | PENDING DEVICE | — |
| Slow Motion / Stabilisierung | PENDING DEVICE | — |
| Thermik/Akku/Dauerlauf | PENDING DEVICE | — |
