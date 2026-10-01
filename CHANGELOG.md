# CHANGELOG

(Pre-existing operator-routine history lives in `master/CHANGELOG.md`.)

## 2026-10-01 — Phase 0 bootstrap (full-stack sync routine)
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, this file.
- Classified repos: `email-messenger` = BACKEND (Spring Boot, Flyway, Thymeleaf, JPA),
  `conexus-mail-mobile` = FRONTEND (Flutter, currently README only).
- Audit finding: no mobile REST API exists; recorded in `docs/API.md` and `INTERNAL_TODO.md`.
- No code changes. Gate: `mvn -q -B verify` exited 0 (all tests passed).
