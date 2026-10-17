# Changelog

## 2026-10-17 — Sync routine, Phase 0 (discovery)
- Classified repos: `email-messenger` = BACKEND (Spring Boot, Flyway, Thymeleaf, JPA);
  `conexus-mail-mobile` = FRONTEND (empty Flutter placeholder).
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, this file.
- Documented the sync audit: no mobile REST API or token auth exists; send logic lives in a controller.
- No production code changed.
- Verified: `mvn -B test` → Tests run: 794, Failures: 0, Errors: 0, Skipped: 1, BUILD SUCCESS.
