# Changelog — email-messenger

## 2026-08-29 — Mobile API bootstrap

- Introduced the mobile-facing REST surface under `/api/v1/*`. Public
  `GET /api/v1/health` is the first endpoint: the Flutter client calls
  it on launch to confirm reachability (`api.HealthController`).
- Security config permits `/api/v1/health` unauthenticated and disables
  CSRF for the whole `/api/**` tree (mobile clients don't participate
  in the browser CSRF flow; each mobile endpoint will authenticate
  explicitly once bearer-token auth lands).
- Added `docs/API.md` (canonical contract with the mobile client),
  `docs/BACKEND.md` (module map + auth model), and `docs/WEB.md`
  (Thymeleaf page inventory).
- Added `CHANGELOG.md` (this file) and `INTERNAL_TODO.md` (gap
  tracker).
- CLAUDE.md now names the sibling repo and each doc's role.
