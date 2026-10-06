# Design- und Bedienprüfung · beta.6

App-weite Durchsicht der Prüfungsansicht, des Stundenplans, der Agenda, des
Notenrechners, der Einstellungen und der Widget-Konfiguration. Ziel sind ruhige
neutrale Flächen, blaue Akzente und verständliche Bedienelemente. Vorhandene
Illustrationen bleiben dekorativ; Informationen stehen auf undurchsichtigen Flächen.

## Nachgewiesene Probleme und Änderungen

Die Ausgangsversion bestand sechs neue Regressionstests **nicht**. Die Tests
rendern echte Android-/Compose-Komponenten bei 320 dp Breite und 160 % Schrift.

| Beobachtung in beta.5 | Verhalten in beta.6 | Prüfung |
| --- | --- | --- |
| „Prüfungen“ in der Navigation abgeschnitten | Bei wenig Platz erhält der aktive Tab mehr Breite; andere Ziele tragen vollständige Screenreader-Namen | Alle fünf Ziele anklicken, Auswahl und vollständigen aktiven Namen prüfen; mindestens 48 dp Zielbreite |
| Ergebniszahl zwischen Filterknöpfen verdrängt | Ergebnis steht bei wenig Platz über den umfließenden Aktionen | Ergebnis sichtbar, beide Beschriftungen vollständig, beide Rückrufe ausgeführt |
| „Gewicht“ bricht mitten im Wort um | Feldpaare wechseln anhand von Breite und Schriftgröße untereinander | Vollständige Beschriftung; ungültiges Gewicht mit zugänglicher Fehlermeldung korrigieren; Fertig beendet den Fokus |
| Schalterzeile nur 32 dp hoch | Ganze Zeile mit tatsächlicher Mindesthöhe 48 dp bedienbar | Höhe, zugänglicher Zustand und genau ein Zustandswechsel |
| „Speichern“ in Widget-Einstellungen bricht um | Aktionen stehen bei wenig Platz untereinander | Vollständige Texte, Abbrechen und unveränderter gespeicherter Konfigurationswert |
| Lange Synchronisationsfehler gekürzt | Kurzer Status mit aufklappbaren vollständigen Details | Vollständiger Fehler ohne Ellipse; Öffnen/Schließen und Zustand nach Wiederherstellung |

Die native Bildkontrolle zeigte außerdem Wortumbrüche bei „Ausblenden“ und
„Nur Prüfungen“ in der Widget-Konfiguration. Vorschauaktionen und Inhaltsauswahl
passen sich jetzt ebenfalls an die verfügbare Breite und Schriftgröße an. Die
Regression prüft die vollständigen Texte und den ausgewählten Inhalt.

Weitere Verbesserungen:

- Einheitliche Kartenradien, dezente Konturen und weniger Schatten; nächste Prüfung
  auf neutraler Fläche mit deutlich hervorgehobener Countdown- und Lernaktion.
- Eingabetext mit 16 sp statt 14 sp, weiterhin mit Nutzerschrift skalierbar.
- Tatsächliche 48-dp-Höhe für Filterchips; sichtbarer Haken für gewählte Filter
  und Auswahlmarkierung bei den Widget-Inhalten. Auswahl ist zusätzlich semantisch verfügbar.
- Überschriften für Screenreader, unterscheidbare Löschaktionen je Notenzeile,
  klare Zustandsangaben für aufklappbare Bereiche und dezente Statusankündigungen.
- Dark-Modus der Komponenten folgt der gewählten App-Palette; er wurde zuvor
  teilweise direkt aus dem Systemmodus gelesen.
- Datenschutztexte erklären Kalenderabruf und Screenshot-Schutz in Alltagssprache.
  Der Projektlink führt direkt zum bestehenden Datenschutzabschnitt. Der bestehende
  Bestätigungsdialog vor dem Löschen lokaler Daten bleibt erhalten.

## Native Ansichten

Diese Aufnahmen stammen aus tatsächlichen Komponenten im Android-Cloud-Renderer
mit synthetischen Schulterminen. Die fünf Tabansichten wurden zusätzlich mit
Navigationsleiste und App-Titel in einem Test-Scaffold zusammengesetzt. Sie zeigen
keine Installation auf einem echten Handy und keine externe Kalenderverbindung.

| Bereich | Hell | Dunkel |
| --- | --- | --- |
| Prüfungen | [Ansicht](screenshots/app-exams-light.png) | [Ansicht](screenshots/app-exams-dark.png) |
| Stundenplan | [Ansicht](screenshots/app-timetable-light.png) | [Ansicht](screenshots/app-timetable-dark.png) |
| Agenda | [Ansicht](screenshots/app-events-light.png) | [Ansicht](screenshots/app-events-dark.png) |
| Noten | [Ansicht](screenshots/app-grades-light.png) | [Ansicht](screenshots/app-grades-dark.png) |
| Einstellungen | [Ansicht](screenshots/app-settings-light.png) | [Ansicht](screenshots/app-settings-dark.png) |

[Große Schrift: Eingaben](screenshots/polish-grades-large-text.png) ·
[Große Schrift: Filter](screenshots/polish-filters-large-text.png) ·
[Große Schrift: Widget-Konfiguration](screenshots/polish-widget-choice-large-text.png) ·
[Hohe Countdown-Ansicht](screenshots/widget-next-launcher-portrait.png).

## Maßstab und tatsächlich geprüfter Umfang

- [Android: Mindestgrößen und zugängliche Standardkomponenten](https://developer.android.com/develop/ui/compose/accessibility/api-defaults):
  mindestens 48 dp für zuverlässige Touchziele. Die geänderten Chip-/Schalter-/Navigationsziele
  werden gegen tatsächliche Layoutgrenzen geprüft, nicht nur gegen unsichtbare Zusatzflächen.
- [Android: Semantik](https://developer.android.com/develop/ui/compose/accessibility/semantics):
  Überschriften, Rollen, Auswahl-/Öffnungszustände, Feldfehler und Statusankündigungen.
- [Android: unterschiedliche Displaygrößen](https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes):
  Anpassung an verfügbare Breite, ergänzt um Nutzerschriftgröße.
- [WCAG 2.2: Textkontrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html):
  neun tatsächlich verwendete Theme-Text-/Flächenpaare in allen vier Hell-/Dunkel-/Kontrastpaletten
  erreichen mindestens 4,5:1. Das ist eine Prüfung dieser 36 Paare, keine vollständige WCAG-Zertifizierung.

Die Leitlinien wurden am 03.10.2026 aus den offiziellen Quellen gelesen.
**134 Tests bestanden**, 0 Fehler, 0 Ausnahmen, 0 Skips in 30 Testsuiten.
Enthalten sind 16 neue UI-/Kontrastprüfungen gegenüber beta.5 sowie die
bestehenden Filter-, Noten-, Widget- und Datenregressionen. Debug-Kotlin,
Debug-/Preview-Lint und Preview-APK-Build bestanden. Lint meldet je Variante
0 Fehler und 32 bestehende Warnungen. 60 native Aufnahmen wurden erzeugt;
Hell-/Dunkelansichten aller fünf Tabs und die relevanten Ansichten bei großer
Schrift wurden visuell kontrolliert. APK-Signatur v2, stabile Testsignatur,
Paket und Versionsmetadaten geprüft.

## Grenzen und Handyprüfung

Echte TalkBack-Ausgabe, Systemtastatur, modale Editor-Bedienung, Launcher-Verhalten
und Darstellung weiterer Schrift-/Displaykombinationen benötigen einen ergänzenden
Handytest. Der bestehende Test für den Termin-Dialog beweist das Öffnen des Android-Dialogs;
sein Compose-Inhalt lässt sich im verwendeten Headless-Renderer nicht zuverlässig anhängen.
Er beweist deshalb keinen ausgeführten Speicherablauf im Dialog.

Kalender-Live-Sync, Benachrichtigungen, Biometrie und externe Datenexporte wurden
mit dieser Revision nicht auf einem Gerät oder gegen private Konten ausgeführt.
Die Änderungen sind UI-bezogen; Import-, Erinnerungs-, Speicher- und Sicherheitslogik
wurden nicht geändert. Tests für bestehende Regeln und die lokale Widget-/DataStore-Integration
laufen zusammen mit den neuen Regressionen.

Zum Handytest: Navigation durch alle Tabs, große Systemschrift einschalten,
Filter setzen und zurücksetzen, Notengewicht korrigieren, Widget-Einstellungen
speichern/abbrechen und Synchronisationsdetails öffnen. Helle Einstellungen sollen
neutrale Flächen und klar blaue Aktionen zeigen. Prüfungen und Termine mit privaten
Daten erst auf dem eigenen Handy verbinden.

GitHub-Veröffentlichung benötigt Schreibzugriff auf Momik-jpg/TestColdown.
Die signierte Preview-APK und ihre Quellen/Nachweise stehen unabhängig davon in Codex Cloud bereit.
