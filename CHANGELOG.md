# CHANGELOG

## 2026-11-08 — Phase 0 bootstrap
- Classified repos: `email-messenger` = BACKEND (pom.xml, Spring, Flyway V1–V35, Thymeleaf); `conexus-mail-mobile` = FRONTEND (README only; no pubspec.yaml/lib/ yet — Flutter project not scaffolded).
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, this changelog; added "Related repositories" to `CLAUDE.md`.
- Sync audit: backend exposes no mobile REST API and no token auth; recorded in INTERNAL_TODO. No code changed.
- Gate: system `mvn -q -B -ntp test` exit 0 (Testcontainers/Docker-dependent tests not runnable here; `./mvnw` wrapper download blocked by sandbox).
