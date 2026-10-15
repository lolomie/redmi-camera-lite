# Datenschutz und Sicherheitsreview

App-Berechtigungen: CAMERA und RECORD_AUDIO; Medienzugriff über MediaStore.
Keine Netzwerkberechtigung, Werbung, Analyse-SDKs, Root oder versteckten
Vendor-Schnittstellen. Medien bleiben lokal, bis Nutzer sie selbst teilen.
Diagnoseexport ist eine bewusste Nutzeraktion. Kamera-/Encoderinformationen
können Modell/Firmware identifizieren; Export vor Weitergabe prüfen.

Keine Medien oder Logcat-Dateien mit privaten Motiven/Ton ins Repository.
Capture-Fehler dürfen keine personenbezogenen Daten oder Tokens protokollieren.
GitHub-Actions erhalten nur Leserechte; Actions sind per Commit gepinnt.
Gradle-HTTPS/Distribution-Prüfsumme und Android-Paketsignaturprüfung bleiben
aktiv. Signing-Keys und Passwörter gehören in sichere Secret-Verwaltung, nicht Git.

Vor Freigabe: Manifest/Lint prüfen, Rechteablehnung und Hintergrundfreigabe
am Gerät testen, Debug-vs-Release-Signatur dokumentieren, APK-Hash und Commit
festhalten. Kein unabhängiges Penetrationstest- oder Sicherheitszertifikat
wird behauptet.
