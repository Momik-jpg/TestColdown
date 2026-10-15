# Durchgehende Lernwelt — Beta 8

Die feste Beta 7 bleibt unverändert. Beta 8 ist eine eigenständige Designüberarbeitung auf derselben lokalen Datenstruktur.

## Gestaltung

Alle fünf Hauptbereiche verwenden eigene GPT-Szenen aus derselben Lernwelt. Die Titel stehen unter den Bildern auf undurchsichtigen Flächen; grosse Schrift reduziert die Bildhöhe, der Kontrastmodus blendet Bilder vollständig aus. Lesbare Sans-Serif-Schrift bleibt in allen Eingaben; die illustrierten Bereichsüberschriften erhalten einen zurückhaltenden Serif-Akzent.

Jade und Petrol kennzeichnen Aktionen, Nebelblau ergänzende Informationen und Messing nur Details. Helle Oberflächen sind kühl und ruhig; dunkle Oberflächen sind marineblau ohne weisse Karten. Karten, Felder, Auswahlchips, schwebende Navigation und Launcher-Symbol teilen dieselbe Formensprache.

Der nächste Prüfungstermin bekommt eine eigene Countdown-Fläche und zeigt einen vorhandenen Raum. Der Notenrechner trennt Zahl, Ergebnis und Anleitung. Stundenplankarten unterscheiden aktuelle Lektionen und Ausfälle; Fehlermeldungen sind als solche erkennbar. Widgets verwenden passende helle und dunkle Farben sowie Messing-/Jade-Orbitringe. Die bestehende Widget-Privatsphäre bleibt bestehen.

## Prüfung

Der [Android-Prüflauf](https://github.com/Momik-jpg/TestColdown/actions/runs/37471498129) prüfte den Quellstand `e1f4f88959623a2895073822a50878d17a5b167c`: 140 Tests in 31 Klassen bestanden, ohne Fehler oder übersprungene Tests. 67 native Compose-/RemoteViews-Ansichten decken die fünf Hauptbereiche, helle/dunkle Darstellung, grosse Schrift, Kontrastmodus, Filter, leere Zustände und Widgets ab. Text-Kontrastpaare bestehen die Grenze 4,5:1. Lint Debug und Preview melden jeweils 0 Fehler, 0 Fatal und 36 Warnungen.

Die Vorschau-APK wurde erfolgreich gebaut. Paketkennung, Version, Offline-Hinweise, V2-Signatur und Inhaltsdigest sind geprüft. Der öffentliche Download liefert HTTP 200, genau 23.742.819 Bytes und SHA-256 `ada8a2280d45bcc8c8603872888382744fdadc7315f16a3f7c20a95350b3c911`.

Die neue Download-Seite zeigt echte native Ansichten mit Beispieldaten und bietet eine einzelne feste APK. Lokale Dateien, Verknüpfungen, Versionsmetadaten und der öffentliche Download wurden geprüft. Browser-QA der Seite war im verwalteten System ohne den erforderlichen control-browser-Skill nicht verfügbar. Ein echter Gerätetest, TalkBack und externe Kalendersynchronisierung bleiben gesonderte Abnahmen.

Das lokale Android-Buildsystem hatte keine vollständigen Maven-Abhängigkeiten; der verifizierte CI-Lauf ist der Buildnachweis. Die finale Liste vermeidet die doppelte Stundenplan-Übersicht; die Wochenansicht behält sie. Der leere Prüfungsbildschirm wiederholt die Kopfillustration nicht. Lange Agendatitel werden vollständig angezeigt.

## Installation

Paket `com.andrin.examcountdown.preview`, Version `1.6.15-beta.8`, Code 31. Das geprüfte Testzertifikat unterscheidet sich von Beta 7. Ein direktes Update von Beta 7 ist damit nicht möglich. Vor einer Deinstallation unter Optionen → Daten eine Sicherung exportieren und aufbewahren; anschliessend nach Installation importieren. Eine Deinstallation löscht lokale Daten. Die Produktions-App bleibt über eine separate Paketkennung unabhängig.
