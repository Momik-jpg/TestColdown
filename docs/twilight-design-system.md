# Twilight Design System

Status: Entwurfsgrundlage für Issue #40

## Ziel

TestColdown soll sich wie ein ruhiger, präziser Prüfungsplaner anfühlen: Die nächste wichtige Prüfung und die anstehende Arbeitslast sind sofort erkennbar. Das visuelle Zeitgefühl erinnert an die Übergangszeit zwischen Tag und Nacht – konzentriert, ruhig und nicht verspielt.

Diese Datei definiert die erste reviewbare Designgrundlage. Sie ändert noch kein Laufzeitverhalten und erfindet keine neuen Produktfunktionen.

## Gestaltungsprinzipien

- **Hierarchie vor Dekoration:** Countdown, nächste Prüfung und Arbeitslast stehen vor sekundären Aktionen.
- **Editorial statt Kartenwand:** Unterschiedliche Inhaltszonen ersetzen eine gleichförmige Reihe identischer Karten.
- **Blau zu Grün:** Blau trägt Orientierung und Zeit; Jade/Teal markieren Fortschritt, Synchronisation und positive Zustände.
- **Ruhige Dringlichkeit:** Amber und Rot erscheinen nur bei echten Warn- oder Konfliktzuständen.
- **Daten zuerst:** Jede sichtbare Kennzahl stammt aus dem bestehenden ExamViewModel, ExamRepository oder den vorhandenen Tab-States.
- **Bestehende Funktionen bleiben erhalten:** Prüfungen, Stundenplan, Events, Erinnerungen, Noten, Widgets, iCal-Sync, Export und Backup werden nur neu angeordnet – nicht entfernt.

## Semantische Farbrollen

Die UI verwendet Rollen statt direkt verteilter Einzelwerte. Die konkreten Werte bleiben in ui/theme/Color.kt und werden über MaterialTheme.colorScheme verwendet.

| Rolle | Hell | Dunkel | Verwendung |
| --- | --- | --- | --- |
| Hintergrund | #F3F6FA | #081827 | App-Fläche |
| Oberfläche | #FFFFFF | #112B40 | Inhaltszonen und Dialoge |
| Erhöhte Oberfläche | #F8FBFD | #16364D | Countdown-Hero, aktive Bereiche |
| Text primär | #132026 | #EDF7F8 | Überschriften und Standardtext |
| Text sekundär | #50606B | #B5C9D0 | Metadaten und Hilfetexte |
| Primär Blau | #0E4C82 | #57B8FF | Navigation, Fokus, Hauptaktion |
| Akzent Jade | #087E68 | #69E5B0 | Fortschritt, Sync-Erfolg, positive Zustände |
| Warnung | #8A5600 | #F2C266 | Zeitdruck oder Konflikt |
| Fehler | #A33B3B | #FF9B8D | Fehlerzustände |
| Trennlinie | #C8D3D8 | #365367 | Grenzen und Divider |

Kontrastziele:

- normaler Text mindestens 4,5:1;
- grosser Text mindestens 3:1;
- Fokusindikatoren und nicht-textliche Bedienelemente mindestens 3:1;
- Farbunterschiede werden nie als einziges Signal verwendet.

Die Werte werden vor dem UI-PR mit dem Accessibility Scanner und Compose-Screenshot-Tests geprüft.

## Komponenten-Inventar

| Komponente | Zweck | Tatsächliche Datenquelle |
| --- | --- | --- |
| AppScaffold | Navigation, globale Aktionen, Snackbar | bestehender Screen-State |
| CountdownHero | nächste Prüfung, Countdown, Datum, Fach, Planen | ExamViewModel / Exam |
| WorkloadSummary | Anzahl und zeitliche Verteilung kommender Prüfungen | gefilterte Exam-Liste |
| AgendaPreview | nächste Lektionen und Events | vorhandene Agenda-/Timetable-States |
| ExamCard | einzelne Prüfung mit Ort, Erinnerung und Aktionen | ExamPresentation / Exam |
| FilterToolbar | Suche, Fach, Zeitraum und Sortierung | bestehende Filterzustände |
| StatePanel | leer, lädt, Fehler, keine Treffer | bestehende Lade-/Fehlerzustände |
| DetailSheet | Prüfung ansehen oder bearbeiten | bestehende Dialoge und Events |

## Dashboard-Zustände

### Daten vorhanden

1. Kopfbereich mit Titel und globaler Sync-Aktion.
2. CountdownHero für die nächste zukünftige Prüfung.
3. WorkloadSummary mit kompakter, verständlicher Zusammenfassung.
4. AgendaPreview mit den nächsten zeitlich relevanten Einträgen.
5. Filter und anschliessend die vollständige Prüfungsliste.

### Keine Prüfungen

Die bestehende EmptyState-Funktion bleibt sichtbar. Sie erklärt kurz, was fehlt, und bietet direkt „Prüfung hinzufügen“ sowie den vorhandenen iCal-Import an.

### Laden, Fehler und keine Treffer

Jeder Zustand erhält eine eigene kurze Erklärung, eine klare nächste Aktion und keinen dekorativen Platzhalter, der den Inhalt verschiebt. Fehler zeigen keine sensiblen iCal-Daten in einer allgemeinen Oberfläche.

## Responsive Verhalten

- Kleine Telefone: eine Spalte, horizontales Scrollen nur für bereits vorhandene Filter, keine abgeschnittenen Karten.
- Grössere Telefone und Tablets: Hero und Zusammenfassung dürfen nebeneinander stehen, wenn die Mindestbreite genügt.
- Sekundärinformationen werden bei knapper Breite unterhalb des Hauptwerts angeordnet.
- Alle wichtigen Aktionen behalten mindestens 44 dp Touch-Fläche.
- Grosse Schrift und System-Zoom dürfen weder Text überdecken noch Aktionen aus dem Viewport drängen.

## Motion und Zustandswechsel

- Countdown-Aktualisierungen bleiben ruhig und blockieren keine Bedienung.
- Keine blinkenden Warnungen, Partikel oder dauerhafte Bewegung.
- Bei Reduced Motion werden Übergänge übersprungen und der stabile Endzustand angezeigt.
- Sync-, Lade- und Fehlerzustände müssen auch ohne Animation verständlich sein.

## Umsetzungsreihenfolge

1. **Theme- und Token-PR:** semantische Rollen, Formen, Typografie und Hell-/Dunkel-Kontrast.
2. **Dashboard-PR:** CountdownHero, WorkloadSummary und AgendaPreview aus vorhandenen States.
3. **Detail- und Zustands-PR:** Prüfung-Detailansicht sowie leer/laden/Fehler/keine Treffer.
4. **Qualitäts-PR:** Compose-Screenshot-Tests, Accessibility-Prüfung, grosse Schrift, Reduced Motion und kleine Bildschirmbreiten.

Jeder Schritt bleibt klein, baut auf dem vorherigen auf und darf die bestehende Daten- und Event-Architektur nicht umgehen.

## Abnahmekriterien für diese Grundlage

- [x] Designrichtung und Twilight-Zeitgefühl dokumentiert.
- [x] Semantische Hell-/Dunkel-Theme-Rollen definiert.
- [x] Dashboard-, Detail- und Zustandsvarianten beschrieben.
- [x] Accessibility- und Responsive-Ziele festgelegt.
- [x] Umsetzung in kleine Jetpack-Compose-PRs aufgeteilt.
- [x] Bestehende Funktionen und Datenquellen ausdrücklich geschützt.
