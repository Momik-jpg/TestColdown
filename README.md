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
