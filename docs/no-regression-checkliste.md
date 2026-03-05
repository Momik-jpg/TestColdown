# No-Regression-Checkliste

## Core
- [ ] App startet ohne Crash.
- [ ] Hell-/Dunkelmodus inkl. Status- und Navigationsleiste ist korrekt.
- [ ] Erststart-Dialog erscheint nur beim echten Erststart.

## Sync
- [ ] iCal-Link-Test funktioniert.
- [ ] Manueller Sync über den Aktualisieren-Button funktioniert.
- [ ] Auto-Sync läuft mit gespeichertem Intervall.
- [ ] Fehlerstatus wird in der Sync-Diagnose angezeigt.

## Daten
- [ ] Manuelle Prüfungen bleiben nach iCal-Sync erhalten.
- [ ] iCal-Prüfungen werden bei erneutem Sync korrekt aktualisiert.
- [ ] Lektionen zeigen Verschiebungen/Ausfälle/Raumänderungen.
- [ ] Event-Import ist nur aktiv, wenn der Schalter aktiviert ist.

## UI
- [ ] Prüfungs-Suche/Filter/Sortierung funktionieren.
- [ ] Kollisionen werden sichtbar markiert.
- [ ] Events-Tab stellt ganztägige Einträge korrekt dar.
- [ ] Notenrechner (Kontrast und Eingabevalidierung) funktioniert.

## Erinnerungen
- [ ] Vorlaufzeiten werden korrekt geplant.
- [ ] Exakte Erinnerungszeit wird korrekt geplant.
- [ ] Snooze-Aktionen funktionieren.
- [ ] Stille Zeiten verschieben Erinnerungen korrekt.

## Backup und Restore
- [ ] Export erzeugt eine gültige JSON- oder verschlüsselte Backup-Datei.
- [ ] Import stellt Prüfungen, Lektionen und Events wieder her.
- [ ] iCal-Link und relevante Einstellungen werden wiederhergestellt.

## Widgets
- [ ] Next-Exam-Widget aktualisiert korrekt.
- [ ] Listen-Widget aktualisiert korrekt.
- [ ] Widget-Refresh triggert Sync/Update.
