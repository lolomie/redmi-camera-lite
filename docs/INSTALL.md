# Build und Installation

JDK 21, Android SDK Platform 35/Build Tools und akzeptierte SDK-Lizenzen
bereitstellen. `ANDROID_HOME` auf das lokale SDK setzen. Aus dem vorhandenen
Checkout bauen; für Cloud-Aufgaben keinen zusätzlichen Worktree anlegen.

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
python3 -m unittest discover -s tools -v
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. CI speichert APK, SHA256SUMS
und COMMIT.txt als Artefakt `redmi-camera-lite-debug` für 14 Tage. Ein Workflow
im Repository ist erst nach Push und erfolgreichem GitHub-Lauf ein CI-Beleg.

USB-Debugging am eigenen Gerät einschalten, RSA-Abfrage bestätigen:

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n de.lolomie.redmicamera/.MainActivity
```

Bei mehreren Geräten `adb -s SERIAL …` verwenden. Signaturkonflikt: bestehende
App/Medien und lokale Daten zuerst prüfen; keine automatische Deinstallation.
Debug-Signaturen sind keine Produktionssignierung. Zum Start Kamera erlauben,
für Videos zusätzlich Mikrofon. Aufnahmen in Galerie/MediaStore kontrollieren.
Danach [DEVICE_TESTS.md](DEVICE_TESTS.md) abarbeiten.

## Automatisierte Gerätetests

Für die vorbereiteten Hardwaretests das Gerät entsperrt und gut beleuchtet
bereithalten. Die Tests erteilen Kamera/Mikrofon, speichern echte Testmedien
und behalten diese zur Sicht-/Hörkontrolle. Hinten wird während der Aufnahme
gedreht. Fehlende Kamera führt zum Skip; ein fehlgeschlagener Aufnahme-/Dateitest
ist kein Erfolg. Der Emulator ist kein Redmi-Nachweis.

```sh
./gradlew --no-daemon assembleDebugAndroidTest
./gradlew --no-daemon connectedDebugAndroidTest
```

Alternativ die beiden APKs installieren und den Runner starten:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w de.lolomie.redmicamera.test/androidx.test.runner.AndroidJUnitRunner
```

Zwei Alltagstests: vorne/hinten JPEG und fünf Sekunden FHD30 mit Audiotrack,
echten PTS und decodierbarem Bild; hinten zusätzlich Rotation.
Vier weitere `SlowMotionDeviceTest`-Tests prüfen die exponierten Zeitlupenmodi
mit etwa fünf realen Sekunden, fehlendem Ton, verlangsamter PTS-Zeitachse und
Framezahl gegen Echtzeit. Clips bleiben gespeichert. Nicht exponierte
Kamera-/FPS-Paare werden geskippt; sonstige Fehler sind echte fehlgeschlagene
Tests. Diese sechs Gerätetests sind in der Cloud nur kompiliert, nicht ausgeführt.
Das prüft weder normale 60 FPS noch native 200 MP oder Dauerthermik.

## Zeitlupe ohne ADB testen

APK 0.2.0 über die vorhandene App installieren (gleicher Debug-Schlüssel).
Oben **Zeitlupe** wählen und zuerst hinten **1280×720 · 120 fps** testen.
Etwa fünf Sekunden ein bewegtes Motiv bei gutem Licht aufnehmen und stoppen.
Dann **Letzte Aufnahme prüfen / Bericht teilen** öffnen. Bericht und sichtbare
Wiedergabe beurteilen; 120 fps mit 30-fps-Wiedergabe erwartet etwa 20 Sekunden,
240 fps etwa 40 Sekunden. Ein `file_criteria_passed=true` ist noch keine
Gerätezertifizierung. Bei Problemen Fehlermeldung und Prüfbericht sichern,
danach über **Kamera** zum normalen Pfad zurückkehren.
