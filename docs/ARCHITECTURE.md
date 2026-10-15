# Architektur

Native Android-App, Kotlin und Jetpack Compose; Android 10/API 29 als Minimum,
compile/target SDK 35. CameraX 1.4.2 verwaltet Preview, JPEG und Recorder mit
Lifecycle-Bindung. Öffentliche Camera2-Metadaten dienen der Diagnose. Keine
Vendor-API, kein Root und kein künstliches Hochskalieren.

Die Oberfläche verwaltet Berechtigungen, Kameraauswahl, Fokus/Zoom, Blitz/Torch,
Foto-/Videozustand und Fehlermeldungen. MediaStore speichert Aufnahmen in der
öffentlichen Medienbibliothek mit Androids Scoped Storage. RECORD_AUDIO wird
für Video benötigt. Kamera und Mikrofon werden beim Verlassen freigegeben;
Aufnahmefinalisierung ist vor Moduswechsel abzuwarten.

CameraX-Videoqualität ist kein FPS-Nachweis. Erweiterte Modi werden anhand von
Kamera-, Stream-, FPS- und Encoderinformationen untersucht. Die Diagnose ist
eine Momentaufnahme der konkreten Firmware, keine Hardware-Zertifizierung.
Camera2-High-Speed-Sessions und Maximum-Resolution-Capture brauchen eine eigene
Implementierung und Gerätebelege, bevor sie als Aufnahmeoption erscheinen.

## Toolchain

JDK 21, Gradle 8.11.1, Android Gradle Plugin 8.9.2, Kotlin/Compose-Plugin 2.1.20.
Versionen sind im Build festgelegt; Gradle-Verteilungsprüfung bleibt aktiv.
CI: Unit-Tests, Android Lint, Debug-APK, SHA-256 und Commit-Nachweis.

## Grenzen

Die Cloud kann Builds und deterministische Tests ausführen. Kameraqualität,
Treiberverhalten, native Sensorauflösung, Dauer-FPS, Audio und Thermik können
nur mit dem exakten Redmi Note 15 Pro 5G samt Firmware geprüft werden.
