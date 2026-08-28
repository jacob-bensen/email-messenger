# Changelog (full-stack sync routine)

Dated entries produced by the full-stack sync routine that keeps this
repo in sync with `../conexus-mail-mobile`. Kept SEPARATE from
`master/CHANGELOG.md`, which is owned by the backend-only autonomous
routine and continues to record its own work independently.

## 2026-08-28
Bootstrap run. Created `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`,
`INTERNAL_TODO.md`, this `CHANGELOG.md`. Added the first REST surface:
`GET /api/v1/health` (`web.api.HealthApiController`) — public,
CSRF-exempt via `/api/v1/**`, returns
`{status,serverTime,apiVersion}`. `SecurityConfig` was updated to
permit `/api/v1/**` and to exempt `/api/v1/**` from CSRF. Slice test
`HealthApiControllerTest` covers the JSON contract.

Advances: Phase 0 discovery + bootstrap. Opens
`docs/API.md`'s "Planned" table for future runs to work through.
