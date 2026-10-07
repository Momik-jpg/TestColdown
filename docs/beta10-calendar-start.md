# Kalenderstart — Beta 10

Beta 10 zeigt den Kalenderstatus in einer eigenen undurchsichtigen Fläche. Hinweise und Aktionen bleiben bei 160 % Schriftgrösse auf einem schmalen Bildschirm lesbar.

- Bei einer fehlgeschlagenen Aktualisierung erscheint «Sync prüfen». Eine frühere erfolgreiche Synchronisierung überschreibt diesen Fehlerstatus nicht.
- «Erneut versuchen» startet die vorhandene Aktualisierung. Bei einem ungültigen oder abgelaufenen Link führt «Link reparieren» zu den Kalender-Einstellungen.
- Ein noch nicht aktualisierter Kalender und ein aktualisierter Kalender ohne gespeicherte Prüfungen erhalten unterschiedliche Hinweise.
- Ohne Kalender-Link erklärt die Hilfe auch die Möglichkeit, Prüfungen manuell anzulegen.
- Statusanzeigen umbrechen statt seitlich ausserhalb der Ansicht zu liegen. Die Aktionen erhalten mindestens 48 dp Höhe; die Überschrift und dynamische Statusmeldung tragen passende Zugänglichkeitssemantik.
- Die drei Überblickskacheln wechseln bei schmaler verfügbarer Textbreite untereinander. Zahlen und Beschriftungen bleiben gemeinsam erkennbar.
- Backup- und CSV-Ausgabe schliessen den zugrunde liegenden Dateistream ausdrücklich. Die gemeinsame Titel-Komponente verwendet die übliche Modifier-Parameterreihenfolge.

## Manuelle Abnahme auf dem Handy

1. Ohne Link die Start-Hilfe öffnen, «Kalender verbinden», «Hilfe» und «Nicht mehr anzeigen» prüfen. Manuelle Prüfungen sind weiterhin möglich.
2. Mit verbundenem Kalender einen fehlgeschlagenen Sync anzeigen: «Sync prüfen» und «Erneut versuchen». Gespeicherte Termine bleiben sichtbar.
3. Einen abgelaufenen Link mit «Link reparieren» korrigieren. Bei Bedarf vorher eine Datensicherung exportieren.
4. Einen Kalender vor dem ersten Sync und einen erfolgreich aktualisierten Kalender ohne Prüfungen vergleichen.
5. Mit grosser Schrift und dunkler Darstellung die Start-Hilfe sowie den Überblick öffnen. Mit TalkBack Überschrift, Status und alle Aktionen prüfen.
6. Eine Backup- und CSV-Datei auf dem Handy exportieren und wieder öffnen.

## Festgelegter Prüfstand

- Quellstand: `4ee5ba51933d6f768210b48cf79597aad2ae0232`.
- [CI-Lauf](https://github.com/Momik-jpg/TestColdown/actions/runs/37569519030): 152 Tests aus 32 Klassen bestanden; 75 native Bildschirmansichten.
- Debug- und Preview-Lint: jeweils 0 Fehler und 33 Warnungen (vorher 36). Die beiden Stream-Warnungen und die Modifier-Parameterwarnung sind behoben.
- Paket `com.andrin.examcountdown.preview`, Version `1.6.15-beta.10`, Code 33, Android 8+; APK 23,748,163 Bytes.
- APK SHA-256: `0b4dfe2a5a3fb90eb68d3151a9d5de45403d3fa8166428afa07575f29ae91a59`.
- Android `apksigner` bestätigt die APK-v2-Signatur. Zertifikat SHA-256: `b3e3d65c3f6ecc5ccf4fbc37339d10ca30cfd4070f975fcd4e2943cf32de563a`.
- Datenschutz-, Zugänglichkeits- und Lizenztexte sind in der APK enthalten.

[Download und Installation](https://testcoldown-work-30min.andrin875272.chatgpt.site): Beim Wechsel von Beta 9 zuerst eine Sicherung exportieren. Das Testzertifikat ist anders; ein direktes Update ist deshalb nicht möglich. Die Deinstallation der alten Test-App löscht lokale Daten.

Native Bildschirmprüfungen nutzen Beispieldaten, die neuen Prüfungen 160 % Schrift. Echte Geräte, TalkBack, Dateianbieter und externe Kalender sind eigenständige, noch offene Abnahmen. Die Download-Seite wurde über Datei- und öffentliche HTTP-Prüfungen kontrolliert; die öffentlichen APK-Bytes und Versionsdaten stimmen mit dem geprüften Build überein. Eine Browser-Abnahme war in der verwalteten Umgebung nicht verfügbar.
