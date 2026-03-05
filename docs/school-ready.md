# School-Ready Check (Kantonsschule-Standard)

## Ziel
Die App ist für private Nutzung und Pilotbetrieb geeignet. Für eine schulweite Nutzung sollten die folgenden Punkte dokumentiert, geprüft und regelmäßig wiederholt werden.

## Datenschutz und Betrieb
- iCal-Links werden lokal gespeichert.
- Die App überträgt keine Daten an Drittserver außerhalb der iCal-Quelle.
- Backup-Dateien können personenbezogene Daten enthalten und müssen geschützt gespeichert werden.
- Logs dürfen keine vollständigen iCal-Links enthalten.

## Qualitätssicherung
- Tests auf mehreren Android-Versionen (mindestens zwei Geräte plus ein Emulator).
- Pflichtabnahme für:
  - Prüfungsimport
  - Lektionen inkl. Verschiebungen/Ausfälle
  - optionalen Event-Import
  - Erinnerungen und Snooze
  - Backup/Restore
  - Widgets

## Barrierefreiheit
- System-Font-Skalierung testen.
- Kontraste in Hell-/Dunkelmodus prüfen.
- Touch-Ziele (Buttons/FAB/Filter) ausreichend groß halten.

## Release und Rollback
- Versionen eindeutig taggen (`vX.Y.Z`).
- APK über GitHub Releases bereitstellen.
- Für kritische Fehler Rollback auf die letzte stabile Release vorbereiten.

## Supportprozess
Fehlermeldungen sollten enthalten:
- App-Version
- Android-Version
- Zeitpunkt
- kurze Reproduktionsschritte
- optional Screenshot
