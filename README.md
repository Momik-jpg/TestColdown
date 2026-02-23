# Exam Countdown (Android)

Android-App für Prüfungs-Countdown mit Home-Screen-Widgets.

## Funktionen
- Prüfungen mit Titel, optionalem Ort und Datum/Uhrzeit speichern
- Sortierte Liste in der App (nächste Termine zuerst)
- Countdown je Prüfung
- Benachrichtigungen mit frei wählbarem Datum und Uhrzeit
- Notenrechner mit gewichteten Noten und Zielnoten-Berechnung
- Noten-Punkte-Rechner (Punkte -> Note, Zielnote -> benötigte Punkte)
- Widget 1: Nächste Prüfung mit Countdown
- Widget 2: Liste der nächsten 5 Prüfungen
- Widgets werden bei Datenänderungen sofort und zusätzlich periodisch aktualisiert

## Tech Stack
- Kotlin
- Jetpack Compose (Material 3)
- DataStore Preferences (Persistenz)
- WorkManager (periodische Widget-Updates und Erinnerungen)
- Klassische AppWidgetProvider + RemoteViews

## Start (lokal)
1. Projekt in Android Studio öffnen.
2. Gradle Sync ausführen.
3. Falls nötig in `local.properties` den SDK-Pfad setzen, z. B.:
   `sdk.dir=C:\\Users\\<DEIN_USER>\\AppData\\Local\\Android\\Sdk`
4. `app` auf Emulator oder Gerät starten.

## Widgets hinzufügen
1. Home-Screen lange drücken.
2. `Widgets` öffnen.
3. `Prüfungs-Countdown` auswählen.
4. `Nächste Prüfung` oder `Prüfungsliste` platzieren.

## GitHub-Verteilung (kostenlos)

### Was du auf GitHub klicken musst
1. Repository öffnen: `https://github.com/Momik-jpg/TestColdown`
2. Oben auf `Actions` klicken.
3. Workflow `Build Android APK` auswählen.
4. Rechts auf `Run workflow` klicken.
5. Branch `main` auswählen.
6. Nochmals `Run workflow` klicken.
7. Warten bis der Lauf grün (`Success`) ist.
8. Ganz unten unter `Artifacts` `ExamCountdown-debug-apk` herunterladen.
9. ZIP entpacken, `app-debug.apk` aufs Handy kopieren und installieren.

### Wenn der Workflow automatisch laufen soll
- Jeder Push auf `main` startet den APK-Build automatisch.

## Play Store Internal Testing (AAB + Signing)
1. Upload-Keystore erzeugen (einmalig):
   `keytool -genkeypair -v -keystore keystore/upload-keystore.jks -alias upload -keyalg RSA -keysize 4096 -validity 10950`
2. `keystore.properties.example` nach `keystore.properties` kopieren und Passwörter eintragen.
3. Signiertes Release-Bundle bauen:
   `.\gradlew.bat :app:bundlePlayRelease`
4. Ergebnis liegt im Projekt unter:
   `dist/ExamCountdown-release.aab`
5. In Google Play Console:
   `App` -> `Testing` -> `Internal testing` -> neues Release -> `.aab` hochladen.

Hinweise:
- Für jeden neuen Upload `versionCode` in `app/build.gradle.kts` erhöhen.
- `keystore.properties` und `keystore/*.jks` niemals committen.
