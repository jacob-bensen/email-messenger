# Internal TODO — email-messenger

Gaps, tech debt, and deferred work. The autonomous full-stack routine
consumes from and appends to this file each run. This is not the
product roadmap — that lives in the roadmap section of `CLAUDE.md`
(features the app should ship) and in `../conexus-mail-mobile/docs/DESIGN.md`
(mobile UX). This file is for internal-quality items and cross-repo
contract gaps.

Order: **contract breakage > logic drift > missing tests > style.**

## Contract gaps (blocks the mobile client)

- [ ] **Mobile-facing auth is not designed.** The website uses form
      login + session cookies; the mobile client cannot use those.
      Design and implement `POST /api/v1/auth/login`,
      `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` with a
      short-lived bearer token + refresh token. Store hashed tokens in
      a new table (`api_tokens`). Update `SecurityConfig` to accept
      `Authorization: Bearer …` on `/api/v1/**` (except `/version`).
      **This is the next feature run's target.**
- [ ] `GET /api/v1/me` — current user (id, email, plan, teams).
      Depends on the auth item above.
- [ ] `GET /api/v1/chats` — mobile-shaped conversation list. Should
      reuse `ConversationListService`; DTO must be defined in
      `docs/API.md` before implementing.
- [ ] `GET /api/v1/chats/{conversationKey}` — one conversation.
      Reuse `ChatService`; DTO must mirror `ChatConversation` /
      `BubbleRun` / `BubbleMessage`.
- [ ] `POST /api/v1/chats/{conversationKey}/reply` — reuse
      `ReplyService` unchanged; DTO in `docs/API.md`.
- [ ] `GET /api/v1/events` — SSE parallel to `/chats/events` for the
      web PWA; same `ChatEventStream` publisher, different auth
      surface.
- [ ] `GET /api/v1/mailboxes` — list connected accounts.

## Logic drift

- [ ] `MailerDaemon` conversations pollute the chat list (roadmap item
      "Delivery states"). If mobile ships before that, the mobile chat
      list will show the same junk conversations. Coordinate.

## Missing tests / hardening

- [ ] Integration test that a request to `/api/v1/version` does NOT
      set a session cookie (mobile should never accidentally get one).
- [ ] Test coverage for the `/api/**` CSRF exemption boundary — a POST
      to `/api/nonexistent` must 404, not 403.

## Style / hygiene

- [ ] `ApiVersionController.VersionResponse` currently reads
      `info.app.version` at bean construction; if we ever want it
      hot-reloadable (unlikely), switch to a lookup at request time.

## Cross-repo audit (as of 2026-08-28)

Full sync audit run today. Findings:

- Backend has NO mobile-facing REST API today, save the new
  `/api/v1/version` smoke test. Every endpoint the mobile client
  needs is planned but not built.
- Mobile client repo is empty until this run's scaffold lands.
- Contract file `docs/API.md` is authoritative from today onward.
