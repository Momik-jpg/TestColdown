# Troubleshooting

## Schwarzer Emulator
1. AVD im Device Manager auswählen.
2. `Cold Boot Now` ausführen.
3. Falls weiterhin schwarz: Graphics auf `Software` stellen.
4. Emulator neu starten.

## iCal-Sync schlägt fehl
- URL prüfen (muss mit `https://` starten).
- Internetverbindung prüfen.
- Bei `HTTP 410`: Link in schulNetz neu generieren.
- Bei `HTTP 401/403`: Zugriffsrechte oder abgelaufener Link.

## Es werden keine Events importiert
- In `iCal-Link verwalten` den Schalter `Events zusätzlich importieren` aktivieren.
- Danach erneut synchronisieren.

## App fragt wieder nach Erststart
- Prüfen, ob der iCal-Link erfolgreich getestet wurde.
- Nach erfolgreichem Test immer `Fertig` drücken.
- Bei sehr aggressiven Akku-Einstellungen App nicht sofort killen.

## Benachrichtigungen kommen nicht
- Android-Berechtigung `Benachrichtigungen` erlauben.
- In-App `Stille Zeiten` prüfen.
- Falls Prüfung schon in der Vergangenheit liegt, wird kein Reminder geplant.
