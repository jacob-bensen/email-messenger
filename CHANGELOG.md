# Changelog

Entries from the full-stack sync routine (backend + `conexus-mail-mobile`).
Earlier history is in [`master/CHANGELOG.md`](./master/CHANGELOG.md).

## 2026-11-10 — Phase 0: discovery and doc bootstrap

No behaviour change. First sync run across both repos.

- Classified this repo as the BACKEND (Maven, Spring Boot, Flyway) and
  `conexus-mail-mobile` as the FRONTEND.
- Added `docs/BACKEND.md` (architecture), `docs/API.md` (canonical HTTP
  contract: the implemented browser surface plus a clearly-marked *proposed*
  mobile API v1), and `docs/WEB.md` (Thymeleaf page inventory).
- Added `INTERNAL_TODO.md` with the sync audit. Headline finding: there is no
  REST API and no token auth, so the mobile client has nothing to call yet;
  the chat send rules live in `ConversationController` and must move into a
  service before a REST send endpoint can share them.
- Added a "Related repositories" section to `CLAUDE.md`.
- Why: the mobile client needs a written contract and a severity-ordered gap
  list before any endpoint is built.
