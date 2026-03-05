# Recurrence Policy (RRULE / EXDATE)

## Current State
- `RRULE` and `EXDATE` are currently **not interpreted** by the importers.
- Importers parse concrete `VEVENT` date fields (`DTSTART`, `DTEND`, optional `DURATION`) and ignore recurrence expansion rules.
- A recurring `VEVENT` may therefore be treated as a single imported instance (the base event), depending on provided fields.

## Scope Impact
- Affects:
  - `IcalImporter` (exams)
  - `TimetableIcalImporter` (lessons)
  - `SchoolEventIcalImporter` (events)
- The app remains stable, but recurrence-heavy calendars can be incomplete or semantically wrong.

## Known Risks
- Missing future instances when calendar source relies on `RRULE` instead of expanded `VEVENT`s.
- Missing exclusions when `EXDATE` is present (cancellations may still appear).
- Potential user confusion when source calendar and app view differ for repeated entries.

## Defensive Handling Plan (Documentation-Only)
- Keep parser behavior deterministic: ignore unsupported recurrence fields, do not crash.
- Surface this limitation in sync diagnostics/help text (planned, not part of this PR).
- Add parser-level detection hooks for `RRULE`/`EXDATE` and telemetry-safe warning counters (planned).
- Re-evaluate with a dedicated recurrence expansion strategy before claiming full iCal recurrence support.

## Review Note
- This document is intentionally policy-only and does **not** introduce runtime behavior changes.
