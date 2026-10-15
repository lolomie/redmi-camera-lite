# Erweiterte Modi: öffentliche API-Machbarkeit

Diese Prüfung unterscheidet einen öffentlichen Implementierungspfad von
tatsächlicher Verfügbarkeit. Ohne konkretes Gerät und Firmware bleiben alle
folgenden Abnahmen `PENDING DEVICE`; es werden keine OEM-Algorithmen zugesagt.

| Funktion | Öffentlicher Ansatz | Freigabebedingung / aktuelle Grenze |
|---|---|---|
| HDR / Nacht / Portrait-Bokeh | CameraX Extensions mit herstellerbereitgestellter Implementierung | `ExtensionsManager.isExtensionAvailable` pro CameraSelector und ExtensionMode; passender Bind-Pfad und echte JPEG-Abnahme. Extension-Bibliothek und UI sind aktuell nicht integriert. |
| Pro: ISO / Belichtungszeit / manueller Fokus | Camera2 `MANUAL_SENSOR`, verfügbare Request-Keys, Sensor-/Objektivbereiche | Kameraeigene Werte und unterstützte Session prüfen; AE/AF-Abhängigkeiten, Exposure-Time/Framedauer und Überbelichtung berücksichtigen. Eigene Session/UX und Geräteabnahme nötig. |
| Weißabgleich | Camera2 AWB-Modi oder `MANUAL_POST_PROCESSING`/Farbkorrektur | Unterstützte Modi und Keys auslesen; kein genereller Kelvin-Regler ohne passende Fähigkeit. |
| ShootSteady | Öffentliche EIS-/OIS-Modi, ggf. Preview-Stabilisierung bei passenden Android-Versionen | Xiaomi-Markenbegriff garantiert keinen Drittanbieterzugriff; unterstützte Kombination aus Kamera, Auflösung und FPS separat aufnehmen und vergleichen. |
| Motion Tracking | CameraX/Camera2 AF oder eigene Analyse nach Kostenprüfung | Kein generischer Zugriff auf Xiaomis proprietäres Tracking. Kontinuierliche Bildanalyse erst nach Performance-/Datenschutzprüfung. |
| Panorama / Zeitraffer / Dokumentmodus | Eigene öffentliche Capture-/Bildverarbeitungspipeline | Zusätzliche Verarbeitung, UX und Tests nötig; kein OEM-Modus durch Kamera-ID allein verfügbar. |

Extensions sind kamera- und geräteabhängig; HDR/Night/Bokeh aus einer
OEM-Featureliste abzuleiten wäre unzulässig. Extensions und beliebige
Foto-/Video-Kombinationen dürfen nicht als frei kombinierbar angenommen werden.
Eigene Camera2-Aufnahmesessions erfordern klaren Übergang mit Freigabe aller
CameraX-Use-Cases; Parallelzugriff auf dieselbe Kamera ist kein Fallback.

Öffentliche Referenzen für die spätere Implementierung:

- https://developer.android.com/media/camera/camerax/extensions-api
- https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics
- https://developer.android.com/reference/android/hardware/camera2/CaptureRequest

Diese Quellen benennen API-Verträge; sie belegen keine Verfügbarkeit auf dem
Redmi Note 15 Pro 5G. Priorität bleiben Alltagspfad und die dokumentierte
Geräteabnahme vor optionalen Modi.
