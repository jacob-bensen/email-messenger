# Changelog

Entries are dated. This file is appended to on every run of the full-stack
sync routine (`jacob-bensen/email-messenger` + `jacob-bensen/conexus-mail-mobile`).

## 2026-08-30

- **Bootstrapped docs system.** Added `docs/BACKEND.md`, `docs/API.md`,
  `docs/WEB.md`, `CHANGELOG.md`, `INTERNAL_TODO.md`. Updated `CLAUDE.md`
  with a `## Related repositories` section pointing at the mobile client.
- **First REST anchor endpoint.** Added `GET /api/v1/health` (`ApiHealthController`
  in a new `com.emailmessenger.api` package). Public (permit-all in
  `SecurityConfig`), returns `{status, service, version, time}`. Slice test
  covers the response body shape. This is the first server → mobile contract
  entry in `docs/API.md`.
