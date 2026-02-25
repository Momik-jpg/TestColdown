# Prüfungs-Planer (Android)
Android-App für Prüfungen, Stundenplan, Events, Benachrichtigungen und Widgets.

## Installation (Handy)
1. Repository öffnen: `https://github.com/Momik-jpg/TestColdown`
2. `Releases` öffnen.
3. Neueste `ExamCountdown-*.apk` herunterladen.
4. APK installieren.
5. Falls nötig: "Unbekannte Apps installieren" erlauben.

## App-Kurzanleitung
1. App starten.
2. Beim Erststart iCal-Link aus deinem Schulkalender einfügen (schulNetz oder anderer iCal-Anbieter).
3. Verbindung testen.
4. Optional: "Events zusätzlich importieren" aktivieren.
5. Fertig drücken.
6. Danach oben mit dem Aktualisieren-Pfeil manuell syncen oder Auto-Sync nutzen.

## Tabs
- `Prüfungen`: Prüfungsliste mit Suche, Filtern, Sortierung, Countdown und Kollisionserkennung.
- `Stundenplan`: Nur Lektionen, inklusive Verschiebungen, Ausfällen und Raumänderungen.
- `Events`: Kalender-Timeline mit Filter "Alles / Nur Prüfungen / Nur Lektionen / Nur Events".
- `Notenrechner`: Durchschnitt, Zielnote und Noten-Punkte-Rechner.

## Neu (Pro-Features)
- `Erste-Schritte Karte`: klare "Was muss ich als Nächstes tun?"-Anleitung direkt im Prüfungs-Tab.
- `Sync-Diagnose`: zeigt letzten Versuch, Dauer, HTTP-Status, Delta-Status, Import-Zahlen und Fehlerursache.
- `Delta-Sync`: nutzt `ETag`/`Last-Modified` (304 = keine Änderungen), spart Akku und Daten.
- `Kollisionsregeln`: getrennte Schalter für Lektionen/Events, nur anderes Fach, echte Zeitüberschneidung.
- `Widget pro Instanz konfigurierbar`: Modus (Prüfungen/Agenda), Zeitraum (7/30/90 Tage), Sortierung.
- `Barrierefreiheit-Modus`: größerer Text + höherer Kontrast.
- `In-App Changelog`: erscheint nach Updates automatisch.
- `CSV/PDF Export`: Prüfungen und Stundenplan direkt aus der App exportierbar.

## Benachrichtigungen
- Mehrere Vorlaufzeiten pro Prüfung möglich.
- Exakte Datum/Uhrzeit-Erinnerung möglich.
- Stille Zeiten und Snooze sind unterstützt.

## Backup
- Über Menü `Werkzeuge`:
  - `Backup Export`
  - `Backup Import`

## Weitere Doku
- Schüler-Kurzanleitung: `docs/kurzanleitung-schueler.md`
- Troubleshooting: `docs/troubleshooting.md`
- School-Ready Betrieb/QA: `docs/school-ready.md`
