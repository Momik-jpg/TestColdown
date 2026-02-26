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

- The app may store school iCal URLs locally on device.
- Treat iCal links as sensitive data and do not share them publicly.
- Backup files can contain personal schedule data.
