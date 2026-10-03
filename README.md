# Prüfungs-Countdown (Android)
Android-App für Prüfungen, Stundenplan, Events, Erinnerungen, Widgets und Notenberechnung.

## Funktionen
- `Prüfungen`: Suche, Filter, Sortierung, Countdown und Kollisionsprüfung.
- `Stundenplan`: Fach-/Raumsuche, Liste/Woche und Filter für Verschiebungen, Ausfälle und Raumänderungen.
- `Agenda`: Liste, Kalender und Tagesansicht mit Filtern für Prüfungen, Unterricht und Termine.
- `Optionen`: durchsuchbare Einstellungen für Kalender, Darstellung, Sicherheit, Daten und Hilfe.
- `Notenrechner`: Durchschnitt, Zielnote und Noten-Punkte-Rechner.
- `Sync-Diagnose`: Status, Dauer, HTTP-Code, Delta-Status und Import-Zahlen.
- `Delta-Sync`: `ETag` und `Last-Modified` zur Reduktion von Datenverkehr.
- `Widgets`: Nächster Eintrag und Terminliste, mit Hell/Dunkel, Raum, Countdown und kompakter Ansicht. Unter `Optionen → Darstellung → Widgets` hinzufügen oder je Instanz einstellen.
- `Export`: CSV/PDF für Prüfungen und Stundenplan.
- `Backup`: Export/Import der App-Daten.

## Installation auf Android
1. Repository öffnen: `https://github.com/Momik-jpg/TestColdown`
2. `Releases` öffnen.
3. Neueste `ExamCountdown-*.apk` herunterladen.
4. APK installieren.
5. Falls nötig: Berechtigung für "Unbekannte Apps installieren" aktivieren.

## Testversion parallel installieren

```bash
./gradlew :app:testDebugUnitTest :app:lintPreview :app:assemblePreview --no-daemon
```

Die Preview-APK liegt unter `.build/app/outputs/apk/preview/app-preview.apk`.
Sie heißt „Prüfungs-Countdown Test“, verwendet `com.andrin.examcountdown.preview`
und die Version `1.6.15-beta.6` (Code 29). Android 8 oder neuer ist erforderlich.
Die eigene Paketkennung ermöglicht eine Installation neben der bisherigen App;
Kalender, PIN und Daten sind getrennt. Die Testversion ist mit dem Android-Testschlüssel
signiert und ersetzt keine mit dem Produktionsschlüssel signierte Installation.

APK auf dem Handy herunterladen und öffnen. Falls Android danach fragt, für den
verwendeten Browser die Installation aus dieser Quelle erlauben. In der Test-App
anschließend den Kalender verbinden oder über `Optionen -> Daten` eine bestehende
App-Sicherung importieren. Bestehende App-Daten werden nicht automatisch übernommen. Diese APK kann die vorherigen
Test-APKs 1.6.15-beta.1/2/3/4/5 mit demselben Testschlüssel aktualisieren; die Test-App-Daten bleiben erhalten.

[Widget-Vorschauen und Prüfumfang](docs/widget-overhaul.md) · [Design- und Bedienprüfung](docs/usability-polish.md) · [Testversion 1.6.15-beta.6](docs/releases/1.6.15-beta.6.md)

## Ersteinrichtung
1. App starten.
2. Beim Erststart iCal-Link einfügen (z. B. schulNetz).
3. `Verbindung testen` ausführen.
4. Optional `Events zusätzlich importieren` aktivieren.
5. `Fertig` drücken.
6. Danach manuell synchronisieren oder Auto-Sync nutzen.

## Erinnerungen
- Mehrere Vorlaufzeiten pro Prüfung.
- Optional exakter Zeitpunkt (Datum/Uhrzeit).
- Snooze und stille Zeiten werden unterstützt.

## Sicherheit und Datenschutz
- iCal-Links werden lokal verschlüsselt gespeichert.
- Es werden nur `https`-Links akzeptiert.
- Sensible URL-Daten werden in Fehlermeldungen redigiert.
- Große iCal-Antworten werden begrenzt.
- Optionaler App-Schutz per PIN und Biometrie.
- Optionaler Screenshot-Schutz über `FLAG_SECURE`.

## Entwicklung
### Voraussetzungen
- JDK 17
- Android SDK (Compile/Target SDK 34)

### Wichtige Befehle
```bash
./gradlew :app:assembleDebug
./gradlew test
./gradlew :app:lintDebug
```

### Signierter Release-Build (AAB)
1. `keystore.properties.example` nach `keystore.properties` kopieren.
2. Keystore-Werte eintragen.
3. Build starten:
   ```bash
   ./gradlew bundlePlayRelease
   ```
4. Ergebnis: `dist/ExamCountdown-release.aab`

## Dokumentation
- Lernwelt-Design, Bedienung und Prüfungen: `docs/study-world-overhaul.md`
- Schüler-Kurzanleitung: `docs/kurzanleitung-schueler.md`
- iCal-Link-Anleitung mit Bild: `docs/ical-link-anleitung-mit-bild.md`
- Troubleshooting: `docs/troubleshooting.md`
- School-Ready Betrieb/QA: `docs/school-ready.md`
- No-Regression-Checkliste: `docs/no-regression-checkliste.md`

## Lizenz
MIT License.  
Details in `LICENSE`.
