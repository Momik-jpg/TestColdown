# Security Policy

## Supported Versions

This project is actively maintained on the `main` branch and through the latest tagged release.

| Version | Supported |
| --- | --- |
| Latest release | Yes |
| `main` | Yes |
| Older releases | No |

## Reporting a Vulnerability

Please use **GitHub Private Vulnerability Reporting** for security issues.

1. Open the repository on GitHub.
2. Go to `Security` -> `Advisories` / `Report a vulnerability`.
3. Submit a private report with:
   - affected version
   - reproduction steps
   - impact
   - possible fix (if known)

If private reporting is not available, open a normal issue only for non-sensitive problems and do not include secrets or private URLs.

## Response Process

- We confirm receipt as soon as possible.
- We reproduce and classify severity.
- We prepare and release a fix.
- We publish details after a fix is available.

## App-Specific Notes

- The app stores iCal URLs locally in encrypted storage.
- Only secure `https` iCal links are accepted.
- Sensitive URL parts are redacted from sync error messages.
- App content is protected against screenshots/screen recording (`FLAG_SECURE`).
- App lock supports PIN and optional biometrics, with lockout after repeated failed attempts.
- Backup files can contain personal schedule data; share only with trusted persons.
