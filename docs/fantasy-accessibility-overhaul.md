# Fantasy-Lernwelt, Barrierefreiheit und Hinweise

Arbeitsstand: 6. Oktober 2026 · Grundlage: Testversion 1.6.15-beta.6 (`39a044c`). Neue Preview: 1.6.15-beta.7, Versionscode 30, eigene Paketkennung `com.andrin.examcountdown.preview`.

## Audit und Umsetzung

Die vorhandenen fünf Hauptbereiche, Import-, Erinnerungs-, Backup-, Schutz- und Widget-Funktionen wurden im Quellcode geprüft. Die Änderungen bauen auf den vorhandenen responsiven Feldern, 48-dp-Schaltern, Fehlersemantiken und High-Contrast-Themes auf.

| Bereich | Gefunden | Änderung |
| --- | --- | --- |
| Gestaltung | Blaue Flächen, unterschiedliche Einleitungen | Zusammenhängende Palette aus Elfenbein, Waldtürkis, Indigo und Messing; stärkere Lesetypografie; originale Sternwarten-Illustration in den Hauptbereichen |
| Navigation | Bei großer Schrift hatten nur aktive Tabs einen sichtbaren Namen | Alle Tabs bleiben beschriftet; adaptive Anordnung in mehreren Zeilen |
| Prüfungen | Lange Titel wurden nach zwei Zeilen gekürzt | Titel können vollständig umbrechen |
| Lesbarkeit | Dekorative Bilder blieben im vereinfachten Modus sichtbar | Banner werden im Modus für höheren Kontrast ausgeblendet; Texte bleiben native, deckend hinterlegte Inhalte |
| Widgets | App-PIN schützt keine Launcher-Inhalte | Neuer pro Widget gespeicherter Privatmodus für Titel und Räume, auch in Screenreader-Beschreibungen; Zeiten bleiben sichtbar |
| Widget-Stil | Abweichende blaue Palette | Passende helle und dunkle Lernwelt-Palette und dezentes natives Sternwarten-Motiv |
| Datenschutzhinweise | Kurze Beschreibung ohne Export-/Launcher-Grenzen | Vollständige Offline-Ansicht mit Zweck, Speicherung, Kalenderabrufen, IP/URL, Berechtigungen, Empfängern, Export, Löschung und Kontaktweg |
| Daten löschen | Bereits angezeigte Erinnerungen konnten sichtbar bleiben | Lokale Erinnerungsbenachrichtigungen werden beim Löschen ebenfalls entfernt |
| Lizenznachweise | Projekt-MIT nur im Repository; ausgeschlossene META-INF-Texte | Generierte, versionsgenaue Runtime-Liste aus Maven-POMs plus Original-LICENSE/NOTICE/COPYING-Dateien, MIT und Apache-2.0-Text in jeder APK/AAB |
| Hilfen | Lesbarkeit nur unter allgemeinen Einstellungen | Eigener auffindbarer Hilfeeintrag mit direktem Sprung zu Ansicht & Bedienung |

## Recherche und rechtliche Einordnung

Grundlagen: Android Compose Accessibility und Semantics (https://developer.android.com/develop/ui/compose/accessibility/api-defaults und https://developer.android.com/develop/ui/compose/accessibility/semantics), WCAG 2.2 (https://www.w3.org/TR/WCAG22/), EDÖB Informationspflicht (https://www.edoeb.admin.ch/de/informationspflicht), Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0) und OpenAI-Ausgabebedingungen (https://openai.com/policies/terms-of-use/).

Es ist keine zusätzliche kommerzielle Bildsammlung eingebunden. Die Bildherkunft wird offengelegt. Angaben zur tatsächlichen Datenverarbeitung werden aus Manifest, HTTPS-Client, lokalem Repository, Backup- und Widget-Code abgeleitet. Die Informationspflicht erfordert verständliche Angaben zu Verantwortlichen, Zweck und Empfängern; vor einer formellen öffentlichen oder schulweiten Verteilung müssen die Betreiberidentität und der verbindliche Kontaktweg bestätigt sowie die im jeweiligen Einsatz geltenden Vorgaben geprüft werden. Der vorhandene öffentliche Name und Projektkontakt sind dokumentiert; private Kontaktdaten wurden nicht erfunden.

Dies ist keine Rechtsberatung, keine Datenschutz-Zertifizierung und keine vollständige WCAG-Konformitätserklärung. Kalenderanbieter, Launcher, Android und gewählte Exportziele bleiben eigenständige Datenempfänger beziehungsweise Plattformen. Ziel-SDK 34 und die Preview-Testsignatur machen diese Ausgabe nicht zu einem fertigen Play-Store-Release.

## Prüfung

Die automatisierte Prüfung wird mit `:app:testDebugUnitTest :app:lintDebug :app:lintPreview :app:assemblePreview` durchgeführt. Neue Tests prüfen die echten APK-Assets, die Offline-Dokumentansicht bei 200 % Schrift, den Bedienungs-Shortcut, fehlende Privatdaten in sichtbaren und vorgelesenen Widget-Inhalten sowie Speicherung und Wiederherstellung des Privatmodus. Die Theme-Prüfung umfasst zusätzliche Sekundär- und Tertiärfarben. Vorhandene Tests zu Import, Erinnerungen, Backups und Navigation werden erneut ausgeführt.

Automatische Prüfung am 6. Oktober 2026: 140 Tests in 31 Testklassen erfolgreich, keine übersprungenen Tests. `testDebugUnitTest`, `lintDebug`, `lintPreview` und `assemblePreview` bestanden. Beide Lint-Berichte enthalten 0 Fehler und 34 Warnungen. Dazu gehören das bestehende Android-Zielniveau, neuere Bibliotheksversionen, teilweise inkompatible Compose-Lint-Erweiterungen, ältere API-Attribute und ungenutzte Ressourcen; die Prüfung deckt deshalb nicht jede Compose-Regel ab. Die Preview-APK enthält Datenschutz, Bedienungshinweise und das vollständige Lizenzinventar (129 aufgelöste Laufzeitbibliotheken, zusätzlich Projekt- und Bildnachweise). Paketkennung `com.andrin.examcountdown.preview`, Version `1.6.15-beta.7`, Code 30.

Die Signaturzertifikate der veröffentlichten Beta 6 und der neuen CI-Test-APK wurden verglichen und unterscheiden sich. Ein direktes Update der alten Preview ist damit nicht möglich. Vor einer notwendigen Neuinstallation sind die bisherigen Test-App-Daten zu exportieren; die Installationshinweise in README.md beschreiben Sicherung und Wiederherstellung.

Native Testbilder verwenden synthetische Kalenderdaten und sind keine Nachweise eines Tests auf einem echten Handy. TalkBack, Schalterzugriff, Tastatur, unterschiedliche Launcher, echter Kalenderabruf und ein Produktions-Signatur-Update müssen auf Geräten geprüft werden. Eine vollständige Barrierefreiheits- oder Datenschutz-Zertifizierung wird damit nicht behauptet.
