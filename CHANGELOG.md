# CHANGELOG

Append a dated entry each routine run.

## 2026-08-31

- Bootstrapped the doc system for the two-repo full-stack routine:
  - Added `docs/BACKEND.md` — architecture and package layout of this repo.
  - Added `docs/API.md` — canonical API contract. Inventories the existing HTML/web
    endpoints for reference and marks the mobile REST API as **TBD**; the mobile
    client cannot yet talk to this backend.
  - Added `docs/WEB.md` — Thymeleaf page inventory, routes, and their services.
- Added `INTERNAL_TODO.md` listing the highest-severity gap: no JSON REST API and
  no token-based auth for a native mobile client.
- Updated `CLAUDE.md` "Related repositories" section to point at
  `conexus-mail-mobile`.
- No production code, migrations, or endpoint behavior changed.
