# School-Ready Check (Kantonsschule-Standard)

## Zielbild
Die App ist für private Nutzung und Pilotbetrieb geeignet. Für schulweite Nutzung sollten die folgenden Punkte dokumentiert und regelmäßig geprüft werden.

## Datenschutz/Betrieb
- iCal-Link wird lokal gespeichert (DataStore).
- Keine Übertragung von Daten an Drittserver durch die App.
- Backup-Dateien enthalten potenziell personenbezogene Daten und müssen geschützt gespeichert werden.
- Logs dürfen keine vollständigen iCal-Links enthalten.

## Qualitätssicherung
- Tests auf mehreren Android-Versionen (mindestens 2 Geräte + 1 Emulator).
- Abnahme für:
  - Prüfungsimport
  - Lektionen + Verschiebungen
  - Event-Import (optional)
  - Erinnerungen/Snooze
  - Backup/Restore
  - Widgets

## Accessibility
- Schriftgrößen (System-Font-Skalierung) testen.
- Kontraste in Light/Dark Mode prüfen.
- Touch-Targets (Buttons/FAB/Filter) ausreichend groß halten.

## Release/Rollback
- Version taggen (`vX.Y.Z`).
- APK via GitHub Release bereitstellen.
- Bei kritischem Fehler auf letzte stabile Release zurückrollen.

## Support-Prozess
- Fehlerreports mit:
  - App-Version
  - Android-Version
  - Zeitpunkt
  - kurzer Reproduktionsbeschreibung
  - optional Screenshot
