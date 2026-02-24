# No-Regression Checkliste

## Core
- App startet ohne Crash.
- Light/Dark Mode passt inkl. Status-/Navigationsleiste.
- Erststart-Dialog erscheint nur beim echten Erststart.

## Sync
- iCal-Link testen funktioniert.
- Manual Sync über Pfeil funktioniert.
- Auto-Sync läuft mit gespeichertem Intervall.
- Fehlerstatus wird im Sync-Balken angezeigt.

## Daten
- Manuelle Prüfungen bleiben nach iCal-Sync erhalten.
- iCal-Prüfungen werden bei erneutem Sync aktualisiert.
- Lektionen zeigen Verschiebungen/Raumänderungen.
- Event-Import nur aktiv, wenn Schalter an.

## UI
- Prüfungsfilter/Suche/Sortierung korrekt.
- Kollisionen werden sichtbar markiert.
- Events-Tab zeigt All-Day sauber an.
- Notenrechner Kontrast und Eingaben korrekt.

## Reminder
- Vorlaufzeiten werden geplant.
- Exakte Erinnerungszeit wird geplant.
- Snooze-Aktionen funktionieren.
- Stille Zeiten verschieben Reminder korrekt.

## Backup/Restore
- Export erzeugt gültige JSON-Datei.
- Import stellt Prüfungen/Lektionen/Events wieder her.
- iCal-Link und Einstellungen werden wiederhergestellt.

## Widgets
- Next-Exam-Widget aktualisiert.
- List-Widget aktualisiert.
- Widget-Refresh-Button triggert Sync/Update.
