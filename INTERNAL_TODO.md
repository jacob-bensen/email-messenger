# INTERNAL_TODO — email-messenger

Findings and deferred work, ordered by severity per the full-stack sync
routine: contract breakage > logic drift > missing tests > style. Consumed
from and appended to on every sync run.

## Contract breakage (blocking mobile client work)

- **No `/api/v1/**` JSON surface exists.** Every controller returns HTML or
  a redirect; the mobile client has nothing to call. Implement the endpoint
  set defined in `docs/API.md` v1.0.0 across auth, mailboxes, conversations,
  search, and events. Start with `POST /api/v1/auth/login`,
  `GET /api/v1/auth/me`, `GET /api/v1/conversations`,
  `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/send`
  — that's the minimum to render a chat and send a reply.
- **No mobile-token issuance path.** Add a `mobile_auth_tokens` table
  (Flyway `V36__mobile_auth_tokens.sql`), a `MobileAuthTokenService`, and a
  Spring Security filter that authenticates `Authorization: Bearer <token>`
  on `/api/v1/**`. Tokens must be opaque, revocable, and stamp last-used-at.
- **No cross-origin / CSRF configuration for `/api/v1/**`.** Web pages use
  CSRF; API must skip CSRF for token-authenticated requests and permit the
  mobile app's origin.
- **SSE mirror `/api/v1/events` is not implemented.** `/chats/events` uses
  session cookies today; port it to token auth.

## Logic drift risk

- Some controllers construct DTO-ish records inline that a JSON API would
  reuse (`ConversationController` → `ConversationListItem`). When adding
  the API, promote these to a shared projection in `service/` so the web
  page and the API return the same shape.
- `ReplyService` always threads onto the thread's last message; mobile
  quote-reply UI will need per-message threading (already tracked in the
  root `CLAUDE.md` roadmap as "Reply-to-a-specific-bubble" — do the API
  side at the same time so `POST /api/v1/conversations/{key}/send` honors
  `inReplyToMessageId`).

## Missing tests

- No slice tests yet for `/api/v1/**` (they don't exist). Add `@WebMvcTest`
  or `@SpringBootTest` coverage for each new endpoint at implementation
  time.

## Style / hygiene

- `docs/BACKEND.md` notes the routine assumes jOOQ; this repo uses JPA. If
  a future migration to jOOQ is desired it should be an explicit epic, not
  a silent drift.
- `master/PLAN.md` and `master/BACKLOG.md` describe the epic-level roadmap
  (EPIC-18 etc.); this sync-routine backlog is orthogonal and about the
  mobile client contract. Keep them separate.

## Recently done

- 2026-08-28 — Bootstrapped `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`,
  root `CHANGELOG.md`, this file, and the "Related repositories" pointer
  in `CLAUDE.md`.
