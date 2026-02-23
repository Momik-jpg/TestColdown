# Exam Countdown (Android)

Android-App für Prüfungs-Countdown mit Home-Screen-Widgets.

## Funktionen
- Prüfungen mit Titel, optionalem Ort und Datum/Uhrzeit speichern
- Sortierte Liste in der App (nächste Termine zuerst)
- Countdown je Prüfung
- Benachrichtigungen mit frei wählbarem Datum und Uhrzeit
- Notenrechner mit gewichteten Noten und Zielnoten-Berechnung
- Widget 1: Nächste Prüfung mit Countdown
- Widget 2: Liste der nächsten 5 Prüfungen
- Widgets werden bei Datenaenderungen sofort und zusaetzlich periodisch aktualisiert

## Tech Stack
- Kotlin
- Jetpack Compose (Material 3)
- DataStore Preferences (Persistenz)
- WorkManager (periodische Widget-Updates und Erinnerungen)
- Klassische AppWidgetProvider + RemoteViews

## Start
1. Projekt in Android Studio oeffnen.
2. Gradle Sync ausfuehren.
3. Falls noetig in `local.properties` den SDK-Pfad setzen, z. B.:
   `sdk.dir=C:\\Users\\<DEIN_USER>\\AppData\\Local\\Android\\Sdk`
4. `app` auf Emulator oder Geraet starten.

## Widgets hinzufuegen
1. Home-Screen lange druecken.
2. `Widgets` oeffnen.
3. `Exam Countdown` auswaehlen.
4. `Naechste Pruefung` oder `Pruefungsliste` platzieren.

## Play Store Internal Testing (AAB + Signing)
1. Upload-Keystore erzeugen (einmalig):
   `keytool -genkeypair -v -keystore keystore/upload-keystore.jks -alias upload -keyalg RSA -keysize 4096 -validity 10950`
2. `keystore.properties.example` nach `keystore.properties` kopieren und Passwoerter eintragen.
3. Signiertes Release-Bundle bauen:
   `.\gradlew.bat :app:bundlePlayRelease`
4. Ergebnis liegt im Projekt unter:
   `dist/ExamCountdown-release.aab`
5. In Google Play Console:
   `App` -> `Testing` -> `Internal testing` -> neues Release -> `.aab` hochladen.

Hinweise:
- Fuer jeden neuen Upload `versionCode` in `app/build.gradle.kts` erhoehen.
- `keystore.properties` und `keystore/*.jks` niemals committen.

## GitHub-Verteilung (kostenlos)
1. Projekt zu GitHub pushen:
   - `git remote add origin https://github.com/<DEIN_USER>/<DEIN_REPO>.git`
   - `git push -u origin main`
2. Nach jedem Push auf `main` baut GitHub Actions automatisch ein APK.
3. Download:
   - GitHub Repository -> `Actions` -> `Build Android APK` -> letzter Lauf
   - Artifact `ExamCountdown-debug-apk` herunterladen.
4. APK auf dem Handy installieren (Unbekannte Apps erlauben).
