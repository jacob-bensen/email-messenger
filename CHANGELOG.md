# Changelog

(Earlier per-feature history lives in `master/CHANGELOG.md`.)

## 2026-10-09 — Phase 0 discovery & doc bootstrap (full-stack sync routine)
- Classified repos: `email-messenger` = BACKEND (Spring Boot, Flyway V1–V35, Thymeleaf); `conexus-mail-mobile` = FRONTEND (Flutter per README, currently only README/.gitignore).
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`; added "Related repositories" to `CLAUDE.md`.
- Audit finding: no mobile REST API or token auth exists; API.md documents only the real non-HTML endpoints and design constraints for a future `/api/v1`.
- No code changed. `mvn -q -B test`: 794 tests, 0 failures, 0 errors, 1 skipped (Docker-only).
