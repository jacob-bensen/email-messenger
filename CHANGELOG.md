# Changelog

## 2026-08-31 — Doc bootstrap for full-stack sync routine

- Added `docs/BACKEND.md` — architecture overview: stack, package layout,
  domain model, email pipeline, auth model, web/REST relationship.
- Added `docs/API.md` — canonical REST API contract for the future mobile
  client. Noted that no REST endpoints ship yet (Thymeleaf-only today) and
  proposed the initial `/api/v1` shape (auth, mailboxes, chats, search,
  attachments, billing, error catalog, SSE stream, server-authority rules).
- Added `docs/WEB.md` — full inventory of Thymeleaf routes, fragments, and
  which controller each route belongs to.
- Added `INTERNAL_TODO.md` — captured the biggest current gap ("no REST API
  exists") plus other sync work.
- Amended `CLAUDE.md` with a "Related repositories" section pointing at
  the mobile client repo `conexus-mail-mobile`.

Why: this run initialized the two-repo full-stack sync routine. The mobile
client repo is still a bare README, so this run focused entirely on
Phase 0 (doc bootstrap + sync audit) instead of a feature. No production
code changed on the backend.
