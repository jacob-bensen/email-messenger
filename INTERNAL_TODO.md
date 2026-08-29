# INTERNAL_TODO — backend

Known gaps, tech debt, and deferred work. Consumed and appended to on
every full-stack-sync run.

## Contract / API surface

- [ ] Auth: no bearer-token exchange yet — the mobile client cannot log
      in. Spec + implementation both blocked. Decide token type
      (session-shared cookie via a mobile-friendly login endpoint, or a
      dedicated bearer-token exchange) and update `docs/API.md` first.
- [ ] `GET /api/v1/me` — current-user profile. Requires auth.
- [ ] `GET /api/v1/mailboxes` — list connected mail accounts. Requires auth.
- [ ] `GET /api/v1/chats` — conversation list, mirroring
      `ConversationListService`. Requires auth.
- [ ] `GET /api/v1/chats/{key}/messages` — messages in one conversation,
      mirroring `ConversationService`. Requires auth.
- [ ] `POST /api/v1/chats/{key}/messages` — reply, through the same
      `ReplyService` the web reply form uses. Requires auth.
- [ ] SSE bridge for the existing `/chats/events` new-mail push, exposed
      as `GET /api/v1/chats/events` for the mobile client.

## Sync audit findings (Phase 0)

- The stack notes in the routine's generic prompt describe **jOOQ**;
  this repo uses **Spring Data JPA + Hibernate**. This is intentional
  (see `docs/BACKEND.md`); no migration planned. Follow the local pattern.
- No REST endpoints existed pre-2026-08-29 aside from the Stripe webhook
  (`billing.BillingWebhookController`). All new mobile endpoints go
  under `api/` with the `/api/v1/` prefix.

## Roadmap items unchanged from `CLAUDE.md`

See the roadmap section at the bottom of `CLAUDE.md`. Support email,
Google OAuth consent-screen publication, Web Push (Live delivery part 2),
delivery states, signature contact card, per-person files/links, and
reply-to-bubble are all still open there — do not duplicate here.
