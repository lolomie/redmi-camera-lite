# Automatisierte Medienprüfung — 15.10.2026

Die folgenden echten Programmläufe verwendeten **synthetische Testbilder und
Sinuston**, keine Kamera. Sie belegen die ffprobe-/Validator-Pipeline, keine
Aufnahmefähigkeit des Redmi. FFmpeg/ffprobe: 7.1.5-0+deb13u1, Systeminstallation.

## Reproduktion

Die Ausgabedateien lagen unter `/tmp` und sind nicht Bestandteil der App oder
des Repositorys. `-n` verhindert Überschreiben vorhandener Dateien.

```sh
ffmpeg -v error -n -f lavfi -i testsrc=size=1920x1080:rate=30 -f lavfi -i sine=frequency=1000:sample_rate=48000 -t 6 -c:v libx264 -threads 2 -preset ultrafast -pix_fmt yuv420p -c:a aac /tmp/redmi-synthetic30.mp4
ffprobe -v error -show_streams -show_frames -of json /tmp/redmi-synthetic30.mp4 > /tmp/redmi-synthetic30.json
python3 tools/validate_media.py /tmp/redmi-synthetic30.json --mode 1080p30
python3 tools/validate_media.py /tmp/redmi-synthetic30.json --mode 1080p60

ffmpeg -v error -n -f lavfi -i testsrc=size=1920x1080:rate=60 -f lavfi -i sine=frequency=1000:sample_rate=48000 -t 6 -c:v libx264 -threads 2 -preset ultrafast -pix_fmt yuv420p -c:a aac /tmp/redmi-synthetic60.mp4
ffprobe -v error -show_streams -show_frames -of json /tmp/redmi-synthetic60.mp4 > /tmp/redmi-synthetic60.json
python3 tools/validate_media.py /tmp/redmi-synthetic60.json --mode 1080p60
python3 -m unittest discover -s tools -v
```

## Tatsächliche Ergebnisse

Alle vier Erzeugungs-/ffprobe-Aufrufe: Exit 0.

| Eingabe / Ziel | Exit | Ergebnis |
|---|---|---|
| 30-FPS-Datei / 1080p30 | 0 | PASS; 180 Frames, 29,999998 FPS, größte Lücke 0,033334 s, 0 geschätzte Fehlframes |
| 30-FPS-Datei / 1080p60 | 1 | Erwartetes FAIL; effektive FPS und Frame-Pacing verfehlen 60-FPS-Ziel |
| 60-FPS-Datei / 1080p60 | 0 | PASS; 360 Frames, 60,000003 FPS, größte Lücke 0,016667 s, 0 geschätzte Fehlframes |
| Python-Unit-Tests | 0 | 11 Tests erfolgreich; darunter fehlendes Dateiende, falsche nominale FPS, fehlende Zeitstempel und Audiolänge |

Beide Dateien: H.264, 1920×1080, Videodauer 6,0 s und Audiodauer 6,0 s.
Der negative 60-FPS-Test darf nicht als Gerätefehler interpretiert werden:
er prüft, dass der Validator eine 30-FPS-Datei nicht als 60 FPS akzeptiert.
Keine Aussage zu Bildqualität, hörbarem Mikrofonton, 200 MP oder Dauerthermik.
