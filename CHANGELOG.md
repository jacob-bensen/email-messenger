# Changelog — email-messenger

Sync-routine changelog. Newest first. This file is distinct from
`master/CHANGELOG.md`, which is owned by the pre-existing operator
routine and tracks product/business ops changes (win-back, retention,
digests, etc.) rather than backend/mobile sync work.

## 2026-08-29

Phase 0 bootstrap of the full-stack sync routine's cross-repo doc
system.

- Added `docs/BACKEND.md` — module map, aggregate roots, DB schema
  overview, auth model, live-delivery pipeline, and the rule that
  the website and future mobile REST surface share the service layer.
- Added `docs/API.md` — the canonical HTTP contract with the mobile
  client. Documents today's `POST /chats/refresh`, SSE
  `GET /chats/events`, Stripe webhook, and lays out the planned
  `/api/mobile/v1/**` surface (auth, me, chats list, chat view,
  reply, read-mark, new chat, live events, push register).
- Added `docs/WEB.md` — full inventory of the Thymeleaf website
  routes with the services each page touches.
- Added `INTERNAL_TODO.md` seeded with the sync audit against the
  current code (main gap: no `/api/mobile/v1/**` endpoints exist yet).
- Updated `CLAUDE.md` with a `Related repositories` section linking
  to `../conexus-mail-mobile` and its `docs/DESIGN.md`, `docs/MOBILE.md`.
