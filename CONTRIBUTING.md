# Contributing Guide

Thanks for contributing to Exam Countdown.

## Quick Start

1. Fork or create a branch from `main`.
2. Make small, focused changes.
3. Run checks locally:
   - `./gradlew :app:compileDebugKotlin --no-daemon`
   - `./gradlew :app:testDebugUnitTest --no-daemon`
4. Open a pull request.

## Commit Style

- Keep commit messages short and clear.
- Prefer one topic per commit.

## Code Style

- Kotlin + Jetpack Compose conventions.
- Keep UI texts user-friendly and consistent.
- Avoid breaking existing import/sync behavior.

## Pull Request Requirements

- Describe what changed and why.
- Add screenshots for UI changes.
- Mention risk areas (sync, reminders, widgets).

## Security

- Never commit secrets or private iCal links.
- Follow `SECURITY.md` for vulnerability reporting.
