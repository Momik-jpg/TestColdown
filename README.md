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

Die feste Beta 11 vereinheitlicht App, Widgets und Symbol mit Elfenbein, Graphit und gezielten Kupferakzenten. Klarere Ecken und zusammenhängende Abschnitte schaffen Ruhe. Die schmale untere Leiste zeigt den aktiven Bereich und «Menü»; alle Bereiche öffnen sich untereinander.

1. [Download-Seite öffnen](https://testcoldown-work-30min.andrin875272.chatgpt.site).
2. Bei vorhandener Test-App zuerst unter `Optionen → Daten` eine Sicherung exportieren und aufbewahren.
3. `ExamCountdown-test-v1.6.15-beta.11.apk` herunterladen und öffnen. Falls Android danach fragt, für den verwendeten Browser die Installation erlauben.
4. Beta 11 hat ein anderes Testzertifikat als Beta 10; ein direktes Update ist nicht möglich. Die alte Test-App erst nach dem Export deinstallieren. **Eine Deinstallation löscht lokale Daten.** Danach installieren und die Sicherung importieren.

Version `1.6.15-beta.11`, Code 34, Paket `com.andrin.examcountdown.preview`, Android 8+. Ein direktes Update benötigt dieselbe Paketkennung und dasselbe Zertifikat. Die Test-App ist unabhängig von der Produktions-App.

Die APK wird über die Download-Seite bereitgestellt. Der [GitHub-Release](https://github.com/Momik-jpg/TestColdown/releases/tag/test-v1.6.15-beta.11) legt den geprüften Quellstand fest. Beta 8, Beta 9 und Beta 10 bleiben unter ihren bestehenden Releases und festen APK-Dateien erhalten.

## Vorschau selbst bauen

```bash
./gradlew :app:testDebugUnitTest :app:lintPreview :app:assemblePreview --no-daemon
```

Die separate Test-APK liegt unter `.build/app/outputs/apk/preview/app-preview.apk`. Der Testschlüssel ersetzt keine mit dem Produktionsschlüssel signierte App.

[Gestaltung und Menü Beta 11](docs/beta11-coherent-design.md) · [Kalenderstart und Abnahme Beta 10](docs/beta10-calendar-start.md) · [Notenrechner Beta 9](docs/beta9-calculator-feedback.md) · [Lernwelt Beta 8](docs/immersive-learning-world-beta8.md) · [Widget-Vorschauen](docs/widget-overhaul.md) · [Bedienprüfung](docs/usability-polish.md)

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
- Vollständige Hinweise: [Datenschutz](PRIVACY.md), auch offline in der App.
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
- Barrierefreie Bedienung: [Schrift, Kontrast, TalkBack und Widgets](docs/accessibility.md)
- Lizenzen: [Bibliotheken und Nachweise](THIRD_PARTY_NOTICES.md)
- Fantasy-Illustrationen: [Herkunft und Nutzung](docs/graphics-provenance.md)
- Lernwelt-Design, Bedienung und Prüfungen: `docs/study-world-overhaul.md`
- Schüler-Kurzanleitung: `docs/kurzanleitung-schueler.md`
- iCal-Link-Anleitung mit Bild: `docs/ical-link-anleitung-mit-bild.md`
- Troubleshooting: `docs/troubleshooting.md`
- School-Ready Betrieb/QA: `docs/school-ready.md`
- No-Regression-Checkliste: `docs/no-regression-checkliste.md`

## Lizenz
MIT License.  
Details in `LICENSE`.
