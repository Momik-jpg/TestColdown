# Prüfungs-Countdown (Android)
Android-App für Prüfungen, Stundenplan, Events, Erinnerungen, Widgets und Notenberechnung.

## Funktionen
- `Prüfungen`: Suche, Filter, Sortierung, Countdown und Kollisionsprüfung.
- `Stundenplan`: Lektionen inkl. Verschiebungen, Ausfällen und Raumänderungen.
- `Events`: Zeitachsen-Ansicht mit Filtern (`Alles`, `Prüfungen`, `Lektionen`, `Events`).
- `Notenrechner`: Durchschnitt, Zielnote und Noten-Punkte-Rechner.
- `Sync-Diagnose`: Status, Dauer, HTTP-Code, Delta-Status und Import-Zahlen.
- `Delta-Sync`: `ETag` und `Last-Modified` zur Reduktion von Datenverkehr.
- `Widgets`: Nächste Prüfung und Liste, pro Instanz konfigurierbar.
- `Export`: CSV/PDF für Prüfungen und Stundenplan.
- `Backup`: Export/Import der App-Daten.

## Installation auf Android
1. Repository öffnen: `https://github.com/Momik-jpg/TestColdown`
2. `Releases` öffnen.
3. Neueste `ExamCountdown-*.apk` herunterladen.
4. APK installieren.
5. Falls nötig: Berechtigung für "Unbekannte Apps installieren" aktivieren.

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
- Schüler-Kurzanleitung: `docs/kurzanleitung-schueler.md`
- iCal-Link-Anleitung mit Bild: `docs/ical-link-anleitung-mit-bild.md`
- Troubleshooting: `docs/troubleshooting.md`
- School-Ready Betrieb/QA: `docs/school-ready.md`
- No-Regression-Checkliste: `docs/no-regression-checkliste.md`

## Lizenz
MIT License.  
Details in `LICENSE`.
