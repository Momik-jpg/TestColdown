# ARCHITECTURE

## 1) Systembild und Datenfluss

```text
Compose Screens/Tabs
    |
    v
ExamViewModel (UiState + Event-Dispatch)
    |
    +--> Domain UseCases (pure logic)
    |      - PlanStudySessionsUseCase
    |      - DetectScheduleCollisionsUseCase
    |      - ComputeTimetableChangesUseCase
    |
    v
ExamRepository (DataStore + Encrypted prefs + backup import/export)
    |
    +--> IcalSyncEngine / SyncCoordinator (network + sync orchestration)
    |       |
    |       +--> IcalImporter / TimetableIcalImporter / SchoolEventIcalImporter
    |
    +--> ReminderScheduler / WorkManager / Widgets
```

**Lesepfad:** Repository-Flows -> ViewModel `stateIn` -> Tab-UiState -> Compose.

**Schreibpfad:** UI-Event -> ViewModel-Handler -> Repository/UseCase -> persistieren -> Flow-Update -> UI-Refresh.

## 2) Warum SyncCoordinator + UniqueWork KEEP

- Mehrere Trigger (manuell, Worker, Widget) können gleichzeitig eintreffen.
- `SyncCoordinator` kapselt Singleflight, damit nur ein echter Sync-Lauf gleichzeitig arbeitet.
- WorkManager läuft mit UniqueWork-Policy `KEEP`, damit bereits laufende Jobs nicht neu gestartet/überschrieben werden.
- Ergebnis: weniger Race-Conditions, weniger unnötige Netzlast, stabilere Delta-Header-Nutzung (ETag/Last-Modified), weniger Write-Churn.

## 3) Warum Europe/Zurich-Fallback + Missing-TZ Handling

- Schulkalender-Daten sind lokalzeit-kritisch (Lektionen/Prüfungen rund um DST-Wechsel).
- Wenn TZ in iCal fehlt oder unvollständig ist, wird auf `Europe/Zurich` normalisiert statt auf Geräte-TZ.
- All-Day-Ende wird lokal als exklusives Enddatum behandelt (keine starre `+24h` Millisekunden-Logik).
- Ziel: keine Off-by-one-Stunde/-Tag Fehler bei DST und All-Day Events.

## 4) Grenzen: UI-only vs. ViewModel/Domain

**UI-only (Compose/Screens):**
- Dialoge, Snackbars, Scroll-/Focus-Verhalten, Navigationszustand, visuelle Filterdarstellung.

**ViewModel:**
- Tab-UiState bündeln, Events dispatchen, Side-Effects koordinieren (Sync, Reminder, Backup-Aufrufe).

**Domain/UseCases:**
- Reine Fachlogik: Session-Planung, Kollisionserkennung, Stundenplan-Change-Klassifikation.
- Keine Android-Framework-Abhängigkeit.

## 5) Backup/Crypto Versionierung (Überblick)

- Backup enthält versioniertes JSON-Format (Schema-Version im Backup-Modell).
- Optional verschlüsselt via AES-GCM mit Passwort.
- Passwort-Härtung über KDF (PBKDF2/Salt) im App-Lock/Backup-Kontext.
- Import ist defensiv: Sanitizing, Begrenzungen, und Fehlerpfade ohne partielles Korruptions-Commit.
- iCal-URLs liegen nicht im Klartext in normalem Storage, sondern in abgesichertem Speicherpfad.
