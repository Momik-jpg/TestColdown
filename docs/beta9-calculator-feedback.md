# Notenrechner — Beta 9

Beta 9 setzt die Lernwelt aus Beta 8 fort. Die vorhandenen Kalenderdaten und die Widget-Konfiguration ändern sich dadurch nicht.

## Bedienung

- Noten im Durchschnitt folgen der sichtbaren Schweizer Skala 1–6. Komma und Punkt sind möglich.
- Eingetragene Noten mit fehlendem oder ungültigem Gewicht erhalten eine Fehlermeldung. Der Durchschnitt und die Kategorie-Schnitte werden erst nach der Korrektur gezeigt; fehlerhafte Zeilen verschwinden nicht still aus dem Ergebnis.
- Ein Zielschnitt und das Gewicht der nächsten Prüfung müssen gültig sein. Die Ergebnisfläche erklärt, ob eine einzige weitere Prüfung genügt, eine 6 nicht mehr reicht oder das Ziel selbst mit einer 1 abgesichert ist.
- Erreichte Punkte liegen zwischen 0 und Maximum. Das Maximum ist positiv. Negative oder überhöhte Punkte ergeben keine scheinbar gültige Note.
- Die frei eingetragene Punkte-Skala muss aufsteigend sein. Ihre Zielnote liegt innerhalb dieser Skala. Der Rechner beschreibt die Umrechnung ausdrücklich als linear.
- Ergebnisse haben eigene, undurchsichtige Flächen mit lesbarer Zahl und Erklärung. Sehr grosse positive Gewichte werden für den Durchschnitt normiert, damit kein Zahlenüberlauf entsteht.

## Manuelle Abnahme auf dem Handy

1. Im Durchschnitt 5 und 3 mit Gewicht 1 eintragen: Ergebnis 4,00. Eine 3 durch 8 ersetzen: sichtbarer Eingabefehler und kein Durchschnitt. Wieder 3 eintragen: Ergebnis kehrt zurück.
2. Bei einer ausgefüllten Notenzeile das Gewicht löschen und anschliessend korrigieren.
3. Mit bestehender Note 2 den Zielschnitt 6 wählen: nächste Einzelprüfung reicht nicht. Mit bestehender Note 6 und Zielschnitt 2: Ziel ist abgesichert.
4. Unter Punkte → Note das Maximum 100 und 60 erreichte Punkte wählen: lineare Note 4,00 bei Skala 1–6. Negative Punkte und 101 Punkte führen zu einem Fehler.
5. Mit grosser Systemschrift, dunkler Darstellung, Kontrastmodus und TalkBack alle Felder sowie die Ergebnisflächen prüfen.

## Festgelegter Prüfstand

- Quellstand: `7d4cc85dd2f4988c68f4eb09f5a53973a5f79906`.
- [CI-Lauf](https://github.com/Momik-jpg/TestColdown/actions/runs/37563673365): 147 Tests aus 31 Klassen bestanden; 70 native Bildschirmansichten. Die neuen Rechner-Prüfungen verwenden 160 % Schriftgrösse.
- Debug- und Preview-Lint: jeweils 0 Fehler und 36 Warnungen.
- Paket `com.andrin.examcountdown.preview`, Version `1.6.15-beta.9`, Code 32, Android 8+; APK 23.746.731 Bytes.
- APK SHA-256: `6e8fc0ea6f76eafa8ef81dfa293eb864a5e739769d68fdd9addca09b0f8a7d70`.
- Android `apksigner` bestätigt die APK-v2-Signatur. Zertifikat SHA-256: `8fc4c2deff5c770644060b32f946faacdfb7f61bcd96497c4a9241d11d29e292`.
- Datenschutz-, Zugänglichkeits- und Lizenztexte sind in der APK enthalten. Die öffentlichen Download-Bytes stimmen mit dem geprüften Build überein.

[Download und Installation](https://testcoldown-work-30min.andrin875272.chatgpt.site): Beim Wechsel von Beta 8 zuerst eine Sicherung exportieren. Beta 9 nutzt ein anderes Testzertifikat; ein direktes Update ist deshalb nicht möglich. Die Deinstallation der alten Test-App löscht ihre lokalen Daten.

Die Bildschirmansichten enthalten Beispieldaten. Ein echter Gerätetest, TalkBack und externe Kalender-Synchronisierung sind gesonderte, noch offene Abnahmen. Eine Browser-Abnahme der Download-Seite war in der verwalteten Umgebung nicht verfügbar; Dateiprüfung und öffentlicher HTTP-Download wurden geprüft.
