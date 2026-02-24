# Prüfungs-Countdown App

## App-Anleitung (Handy)
1. Öffne das Repository: `https://github.com/Momik-jpg/TestColdown`
2. Öffne `Releases`.
3. Lade die neueste `ExamCountdown-debug-<version>.apk` herunter.
4. Installiere die APK auf deinem Handy.
5. Falls nötig: Erlaube `Unbekannte Apps installieren`.

## iCal-Import (schulNetz)
1. Öffne in der App den Tab `Prüfungen`.
2. Tippe oben rechts auf das Wolken-Symbol (`iCal importieren`).
3. Füge deinen schulNetz-iCal-Link ein.
4. Tippe auf `Importieren`.
5. Prüfungen und Stundenplan werden synchronisiert.
6. Im Stundenplan werden nur Lektionen angezeigt (keine `Termin`-Events).
7. Danach synchronisiert die App automatisch im Hintergrund (bei Internet).
8. Der iCal-Link bleibt gespeichert. Oben kannst du mit dem Pfeil (`Aktualisieren`) manuell neu laden.

## Stundenplan
- Neuer Tab `Stundenplan` mit Tagesansicht.
- Zeigt nur Lektionen (keine `Termin`-Events).
- Verschobene Lektionen werden markiert.
- Alte Zeitfenster bei Verschiebungen werden durchgestrichen angezeigt.
- Raumänderungen werden als `alter Raum -> neuer Raum` angezeigt.
- Zeiten werden in `Europe/Zurich` angezeigt.
- Wird zusammen mit dem iCal-Import automatisch synchronisiert.

## Design
- Die App unterstützt Hell- und Dunkelmodus (abhängig vom Gerätemodus).

## Hinweis
- Beim erneuten Import werden alte iCal-Einträge aktualisiert, manuell erstellte Prüfungen bleiben erhalten.
