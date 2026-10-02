# Lernwelt: Design und Abnahme

Die Android-App erhält eine ruhige Lernwelt: warmes Papier, Kobaltblau, Teal und
kleine goldene Akzente. Der Dunkelmodus verwendet dunkle, klar getrennte Flächen.
Material-Komponenten behalten ihre Bedienung und Kontrastfarben; die vorhandenen
barrierearmen Farbpaletten bleiben erhalten.

## Bedienung

- Material-Navigation am unteren Rand mit ausgewähltem Tab und beschrifteten Zielen.
- Der Header benennt den aktuellen Bereich; das Aktionsmenü ist auch im Notenrechner erreichbar.
- Prüfungen beginnen mit einer Illustration und der zeitlich nächsten Prüfung.
  „Lernen planen“ ist eine beschriftete Hauptaktion. Die gewählte Sortierung gilt
  für die übrige Liste, nicht für die Auswahl der nächsten Prüfung.
- Vergangene Prüfungen bleiben in „Alle“ sichtbar, werden aber nicht als nächste
  Prüfung bezeichnet. Zeitraum und Spotlight werden alle 30 Sekunden neu geprüft.
- Suche berücksichtigt die dargestellten Titel, Fach und Raum. Erweiterte
  Filter sind einklappbar. „Filter zurücksetzen“ stellt alle Standardwerte wieder her.
- Filter, Tab-Zustände und Noten-Eingaben bleiben beim Tabwechsel erhalten.
  Noten-Eingaben unterstützen Androids Wiederherstellung nach Rotation/Prozessende;
  dies ist kein dauerhaftes Notenarchiv.
- Zahlenfelder verwenden eine Dezimaltastatur. Komma und Punkt sind zulässig,
  nicht endliche Zahlen werden abgewiesen.

## Kompakte Überarbeitung

- Weniger Einleitungstext, kleinere Überschriften und Abstände. Die dekorative
  Landschaft ist 76 statt 116 dp hoch; Material-Eingabe- und Touchgrößen bleiben erhalten.
- Alle Eingaben nutzen einheitliche Konturen, Rundungen und Theme-Farben. Die
  Tastatur bietet „Weiter“, „Fertig“ oder „Suchen“; Zahlenfelder passende Zahlentastaturen.
- Stundenplan: Fach und Zeit in einer Zeile, Raum direkt darunter. Raumwechsel,
  Verschiebungen und Ausfälle bleiben beschriftet. Die Filter lassen sich einklappen.
- Die Wochenkarten sind schmaler, enthalten Räume und Änderungen und öffnen die
  aktuelle Woche beim heutigen Wochentag. Andere Wochen beginnen links am Montag.
- Agenda: kurze Überschriften und eine kompakte Ansichts-/Filterzeile. Ein erster
  eigener Termin ist jetzt auch ohne vorhandene Kalenderdaten oder iCal-Link möglich.
- Notenrechner: Ziel/Schnitt und Punktewerte nebeneinander; Kategorie-Eingaben
  öffnen bei Bedarf. Ungültige Noten und Zeilengewichte werden direkt erklärt.

## Illustrationen

`study_landscape.png` und `study_empty.png` wurden am 02.10.2026 mit ChatGPTs
Bildgenerierung eigens für diese Überarbeitung erzeugt. Die Motive sind ein
Bergpfad zur Sternwarte und eine schwebende Insel mit Notizbuch. Die Dateien
liegen unter `app/src/main/res/drawable-nodpi` und funktionieren offline.
Dekorative Bilder haben keine TalkBack-Beschreibung. Texte und Aktionen stehen
auf eigenen undurchsichtigen Theme-Flächen, nicht über den Bildern.

## Prüfen

```sh
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

`ExamFilterPolicyTest` prüft Zeitfenster, Suche, Reset und die nächste Prüfung bei
unterschiedlichen Sortierungen. `GradeNumberPolicyTest` prüft Zahleneingaben.
`StudyWorldUiTest` rendert echte Compose-Controls mit synthetischen Daten und
prüft Hauptaktionen, Hell/Dunkel, erhöhten Kontrast sowie wiederhergestellte
Noten-Eingaben, Stundenplan-Filter, Raumwechsel, optionale Kategorien und
Tastaturaktionen. Beim Termindialog wird das Öffnen des echten Android-Dialogs
überprüft. Die PNGs landen unter `STUDY_UI_ARTIFACTS` beziehungsweise
`build/study-ui`. Android-QA lädt sie mit den Testberichten hoch.

Eine Geräteabnahme von Live-Sync, Benachrichtigungen, Widgets und Backup bleibt
zusätzlich nötig; synthetische Oberflächentests beweisen keine externe Anbindung.

### Tatsächlich ausgeführt am 02.10.2026

In Codex Cloud mit JDK 17, Android SDK 34 und Gradle 8.2:
**79 Tests bestanden, keine Fehler oder Skips**. Kotlin-Kompilierung,
Android-Lint und Debug-APK-Build erfolgreich; `git diff --check` bestanden.
Die neue Abdeckung umfasst sieben Filter-/Zahleneingabe-Regressionen und zwölf
Compose-Tests. Die kompakte Revision ergänzt sechs Compose-Tests für Stundenplan,
Agenda, Tastaturwechsel, Fehlermeldungen und Kategorien. Die übrigen 60 vorhandenen Tests bestanden ebenfalls.

Maven Central lieferte in dieser Umgebung HTTP 429. Für diese lokale Prüfung
wurde ausschließlich über ein temporäres Gradle-Init-Skript Googles
Maven-Central-Spiegel verwendet. Repository-Repositories und Versionsvorgaben
wurden dadurch nicht geändert. Proxy-CA und beschreibbare Cache-/SDK-Pfade
wurden nur in der isolierten Buildumgebung unter `/tmp` eingerichtet.

Zusätzlich wurde versucht, im Robolectric-Termindialog Titel/Ort einzugeben und
zu speichern. Auf API 28 und 33 blieb dessen Compose-Root beim Fenster-Anhängen
stehen (`AppNotIdleException`; Diagnose: `pending setContent`). Dieser zusätzliche
Ablauf ist **nicht bestanden bzw. nicht nachgewiesen**. Die zwölf bestandenen
Compose-Tests enthalten deshalb eine ausdrücklich begrenzte Prüfung des
Dialog-Öffnens, keine Speichern-Abnahme. Die Rohdaten des letzten Zusatzversuchs
liegen lokal unter `artifacts/dialog-render-attempt/`; diese Grenze wird nicht als
bestätigter Produktfehler oder bestandene Live-Anbindung ausgegeben.

Die folgenden neun Aufnahmen zeigen echte native Compose-Komponenten mit
synthetischen Daten, keine auf einem Telefon gestartete vollständige Sitzung.
Hell/Dunkel, Leerzustände, Stundenplan, Agenda und Notenrechner wurden visuell kontrolliert.

| Prüfungen hell | Prüfungen dunkel |
| --- | --- |
| ![Prüfungen hell](screenshots/exams-light.png) | ![Prüfungen dunkel](screenshots/exams-dark.png) |

| Leerzustand | Notenrechner nach Wiederherstellung |
| --- | --- |
| ![Leerzustand](screenshots/exams-empty.png) | ![Notenrechner](screenshots/grades-light.png) |

| Stundenplan | Wochenansicht |
| --- | --- |
| ![Stundenplan](screenshots/timetable-light.png) | ![Wochenansicht](screenshots/timetable-week.png) |

| Agenda | Leere Agenda |
| --- | --- |
| ![Agenda](screenshots/agenda-light.png) | ![Leere Agenda](screenshots/agenda-empty.png) |

## gg-Arbeitsweise

Die Planung orientiert sich am `gg`-Skill aus Codex-Simple-Accounts. Der aktuelle
Cloud-Chat registriert dessen MCP-Tool nicht. Der Benutzer hat ausdrücklich eine
Simulation erlaubt. Deshalb sind dies ein manuell erstellter Arbeitsplan und
beobachtete Prüfergebnisse, keine vom Router erzeugten Scores oder nachgewiesene
Modellumschaltung. Die gg-Integration wurde nicht installiert; bestehende Skills,
Plugins und persönliche Konfigurationen bleiben erhalten.
