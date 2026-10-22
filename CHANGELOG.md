# Changelog

(The earlier per-feature log lives in `master/CHANGELOG.md`.)

## 2026-10-22 — Full-stack sync, Phase 0 (discovery)
- Classified repos: `email-messenger` = BACKEND (Spring Boot, JPA, Flyway, Thymeleaf);
  `conexus-mail-mobile` = FRONTEND (currently README only, no Flutter project).
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, this file;
  added "Related repositories" to `CLAUDE.md`. Docs only; no code or schema changes.
- Findings: no client REST API or token auth exists; project uses JPA, not jOOQ;
  `ConversationController` holds orchestration logic that must move to services before an API is added.
- Baseline `mvn -B test`: 794 run, 0 failures, 0 errors, 1 skipped.
