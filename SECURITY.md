# Sicherheitsrichtlinie

## Unterstützte Versionen
Dieses Projekt wird aktiv auf `main` sowie in der neuesten Release-Version gepflegt.

| Version | Unterstützt |
| --- | --- |
| Neueste Release | Ja |
| `main` | Ja |
| Ältere Releases | Nein |

## Sicherheitslücke melden
Für sicherheitsrelevante Meldungen bitte **GitHub Private Vulnerability Reporting** nutzen.

1. Repository auf GitHub öffnen.
2. `Security` -> `Advisories` bzw. `Report a vulnerability` öffnen.
3. Privat melden mit:
   - betroffener Version
   - Reproduktionsschritten
   - Auswirkung/Impact
   - möglichem Fix (falls bekannt)

Falls Private Reporting nicht verfügbar ist, nur nicht-sensitive Probleme als normales Issue melden und keine Geheimnisse/private URLs veröffentlichen.

## Reaktionsprozess
- Eingang wird so schnell wie möglich bestätigt.
- Problem wird reproduziert und priorisiert.
- Fix wird vorbereitet und veröffentlicht.
- Details werden nach Bereitstellung eines Fixes offengelegt.

## App-spezifische Sicherheitsaspekte
- iCal-Links werden lokal verschlüsselt gespeichert.
- Es werden nur `https`-iCal-Links akzeptiert.
- Sensible URL-Bestandteile werden in Fehlern redigiert.
- Optionaler Screenshot-/Screenrecord-Schutz über `FLAG_SECURE`.
- App-Schutz via PIN und optional Biometrie.
- Backup-Dateien können personenbezogene Daten enthalten und sollten nur vertrauenswürdig geteilt werden.
