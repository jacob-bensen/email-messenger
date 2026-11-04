# CHANGELOG

## 2026-11-04 — Phase 0 bootstrap (docs only, no code changes)
- Classified repos: `email-messenger` = BACKEND (Spring Boot, Flyway, Thymeleaf); `conexus-mail-mobile` = FRONTEND (empty, README only).
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`; sync audit found no mobile API exists.
- Backend `./mvnw -q -B test`: exit 0; 794 run, 0 failures, 0 errors, 1 skipped. Frontend gates not run (no Flutter project or SDK).
