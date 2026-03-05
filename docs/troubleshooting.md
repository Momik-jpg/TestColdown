# Troubleshooting

## Schwarzer Emulator
1. AVD im Android Device Manager auswählen.
2. `Cold Boot Now` ausführen.
3. Falls weiterhin schwarz: Grafikmodus auf `Software` stellen.
4. Emulator neu starten.

## iCal-Sync schlägt fehl
- Prüfen, ob die URL mit `https://` beginnt.
- Internetverbindung prüfen.
- Bei `HTTP 410`: Link in schulNetz neu erstellen.
- Bei `HTTP 401/403`: Zugriffsrechte prüfen oder neuen Link erzeugen.
- Bei `HTTP 304`: Kein Fehler, es wurden nur keine Änderungen gefunden.

## Keine Events importiert
- In der Kalender-Konfiguration `Events zusätzlich importieren` aktivieren.
- Danach erneut synchronisieren.

## Erststart erscheint erneut
- Prüfen, ob der iCal-Link-Test erfolgreich war.
- Nach erfolgreichem Test immer `Fertig` drücken.
- Bei aggressiven Akku-Einstellungen die App nicht direkt beenden.

## Benachrichtigungen kommen nicht
- Android-Berechtigung `Benachrichtigungen` erlauben.
- In-App `Stille Zeiten` prüfen.
- Erinnerungen für vergangene Termine werden nicht neu geplant.
- Sicherstellen, dass die Prüfung eine gültige Vorlaufzeit oder exakte Zeit gesetzt hat.
