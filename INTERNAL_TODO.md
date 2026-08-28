# INTERNAL_TODO

Running list of known gaps, tech debt, and deferred work discovered by the
full-stack sync routine. Ordered top → bottom by severity: **contract
breakage** first, then **logic drift**, then **missing tests**, then
**style**.

Also see `master/BACKLOG.md` and `master/MASTER_ACTIONS.md` for the
website/back-office roadmap that predates the mobile-client work.

## Contract breakage (P0)

- **No REST API exists.** `docs/API.md` describes the intended surface,
  but zero endpoints are implemented on the backend. The mobile repo
  (`conexus-mail-mobile`) is empty and blocks on this. Bootstrap in this
  order:
  1. Add `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/me` plus
     bearer-token auth alongside the existing `SecurityConfig` session
     stack.
  2. Add `/api/v1/chats` (list) and `/api/v1/chats/{key}` (bubbles),
     reusing `ConversationListService` and `ChatService` unmodified.
  3. Add `/api/v1/chats/{key}/messages` (send) via `ReplyService` +
     `OutboundMessageService`.
  4. Add `/api/v1/mailboxes` CRUD via `MailAccountService`.
  5. Add the live push endpoint. `ChatEventStream` already fans out to
     SSE for the web; either expose the same SSE at `/api/v1/chats/events`
     with bearer auth or add a WebSocket sibling for mobile.
- **Bearer-token auth is unimplemented.** Every planned API endpoint
  requires it; today only cookie sessions exist.

## Logic drift (P1)

- **Attachment blob storage.** `Attachment.blob_ref` is always null;
  attachments can't be downloaded from the web today and cannot be at all
  from a future mobile client. Blob storage is on the CLAUDE.md roadmap
  ("Per-person shared files & links"). Any mobile UI that shows
  attachments must not ship until this lands.
- **Web Push notifications (roadmap item).** Once implemented via VAPID,
  the mobile client will want the equivalent through FCM/APNs. Design the
  push-subscription table so it stores platform + token + endpoint from
  day one, not just browser subscriptions.
- **Reply-to-a-specific-bubble (roadmap item).** `ReplyService` threads
  onto the last message in the thread; the mobile compose UI should not
  assume per-bubble replies until the backend supports it.
- **Delivery states for outbound bubbles (roadmap item).** The mobile
  bubble UI will want `Sending / Sent / Delivered / Not Delivered`; the
  API needs to expose that per-message enum once the backend does.

## Missing tests (P2)

- No integration tests exercise the REST surface (because it doesn't
  exist yet). When endpoints land, add slice tests (`@WebMvcTest`) plus a
  Testcontainers-backed end-to-end covering login → list chats → send.
- IMAP IDLE (`ImapIdleService`) is only smoke-tested in tests; the
  CLAUDE.md roadmap notes it "not yet verified against a real IMAP
  server that supports IDLE". The mobile live-push feature depends on
  IDLE working, so verify against a real Gmail/Fastmail mailbox before
  wiring it through the mobile client.

## Style / hygiene (P3)

- The `master/` directory at repo root (with `BACKLOG.md`, `PLAN.md`,
  `MASTER_ACTIONS.md`, `CHANGELOG.md`) shares its name with the git
  branch `master`. It's an operator-notes directory from a prior
  autonomous routine, not a git worktree. Consider renaming to `ops/` or
  `operator/` to remove the ambiguity — but this is cosmetic; leave
  alone unless doing an unrelated docs pass.
- The routine's generic prompt says the stack uses jOOQ; this backend
  uses Spring Data JPA. Follow the code, not the routine's assumption.

## Newly discovered this run

- Both `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md` were missing and
  have been created (2026-08-28).
- The sibling mobile repo `conexus-mail-mobile` is essentially empty
  (only `.gitignore` + `README.md`) — Flutter scaffolding is a task in
  itself. Flutter/Dart tooling is NOT installed in this environment; the
  first mobile-side run will need to either install it or scaffold
  by-hand.
