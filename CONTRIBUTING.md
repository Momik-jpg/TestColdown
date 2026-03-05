# Beitragsrichtlinien

Danke für Beiträge zu Prüfungs-Countdown.

## Schnellstart
1. Branch von `main` erstellen (oder Fork verwenden).
2. Änderungen klein und thematisch fokussiert halten.
3. Lokale Checks ausführen:
   - `./gradlew :app:compileDebugKotlin --no-daemon`
   - `./gradlew :app:testDebugUnitTest --no-daemon`
   - `./gradlew :app:lintDebug --no-daemon`
4. Pull Request eröffnen.

## Commits
- Pro Commit nur ein Thema.
- Aussagekräftige, kurze Commit-Nachrichten.

## Code- und UI-Qualität
- Kotlin/Jetpack-Compose-Konventionen einhalten.
- Bestehendes Import-/Sync-Verhalten nicht unbeabsichtigt ändern.
- Nutzertexte konsistent und verständlich formulieren.

## Anforderungen an Pull Requests
- Ziel und technische Änderung klar beschreiben.
- Bei UI-Änderungen Screenshots hinzufügen.
- Risiken benennen (z. B. Sync, Reminder, Widgets, Backup).
- Wenn möglich kurz angeben, welche Tests manuell/automatisch geprüft wurden.

## Sicherheit
- Keine Secrets, Tokens oder private iCal-Links committen.
- Für Sicherheitslücken den Prozess in `SECURITY.md` verwenden.
