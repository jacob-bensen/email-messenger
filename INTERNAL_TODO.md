# INTERNAL_TODO

Contract gaps, tech debt, and deferred work discovered by the sync
routine. Severity-ordered: contract breakage > logic drift > missing
tests > style. Newer items may go under existing sections.

## Contract breakage (blocks the mobile client)

- **No JSON REST API exists for `conexus-mail-mobile`.** Every mobile
  screen (login, chat list, conversation, mailbox connect, subscription)
  needs a matching backend endpoint. See `docs/API.md` §Planned for the
  starter list. Nothing in the mobile client can be implemented against
  the current backend as-is except SSE, and even that requires a
  bearer-token auth path.
- **No bearer-token / API-token auth flow.** Backend auth is session-cookie
  based (form + Google OIDC). A mobile client cannot use cookies as a
  primary auth. Blocker for every mobile endpoint. Design: opaque token
  issued per device on sign-in, `Authorization: Bearer <token>` on every
  request, revoke on password change and explicit sign-out.
- **`/chats/events` SSE stream is session-cookie authed.** Once bearer
  tokens ship, either accept `Authorization` on this stream or add
  `GET /api/v1/events` alongside it that shares `ChatEventStream` under
  the hood.

## Logic drift risks (once the mobile client starts existing)

- **Business logic in Thymeleaf controllers must be pushed into services
  before any REST endpoint reuses it.** Audit
  `ConversationController`, `MailboxController`, `AccountController`,
  `BillingController` for logic that lives in the controller body rather
  than a `@Service`. Any duplication between web and API paths is a
  future divergence.
- **Server-authoritative reads for the mobile client.** Do not send raw
  entity graphs — build response DTOs (records) in a `web.api` package so
  the wire shape is stable regardless of JPA changes.

## Backend roadmap follow-ups (already in `CLAUDE.md`)

Not repeated here; see the roadmap list in `CLAUDE.md`. Highest severity
that touches the mobile client contract: **Web Push (VAPID)** — mobile
will want push too, so pick a scheme that works for both PWA push and
native push (FCM/APNs) before implementing either.

## Tech debt

- `Attachment.blob_ref` is always null — attachment bytes aren't stored.
  Both web download endpoint and any mobile attachment view will need
  this. See roadmap "Per-person shared files & links".
- `ReplyService` always threads onto the thread's last message, mis-threading
  replies to older bubbles. Already tracked in roadmap.
- MAILER-DAEMON pseudo-conversations pollute the chats list. Already
  tracked in roadmap under delivery states.

## Documentation

- `docs/API.md` has TBD placeholders for every planned endpoint. Each must
  become a real schema before the corresponding client code lands.
- `docs/WEB.md` names each route's controller by inference; verify the
  exact controller class the first time the page is touched and correct
  any drift.

## Test coverage

- No integration test yet asserts that the SSE stream is session-cookie
  authed (only that it emits events). Worth adding once bearer-token auth
  ships so we don't regress.
