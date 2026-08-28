# Internal TODO — email-messenger backend

Running gap list. Consume from and append to this on every sync-routine
run. Ordered by severity per the routine's convention:
**contract breakage > logic drift > missing tests > style.**

## Contract-level gaps (block the mobile client)

The mobile app cannot be built until the backend exposes a real REST
surface. Today the whole app is HTML forms with cookie sessions and
zero token auth. The following must ship (in `docs/API.md` order),
each in its own feature run:

- [ ] **Token-based auth.** Pick between short-lived JWT + refresh
      token and opaque session token backed by a `mobile_sessions`
      table. Ship the login / register / logout endpoints under
      `/api/v1/auth/*`. Filter must recognize both `Authorization:
      Bearer` and the existing cookie session (both hit the same
      `User` principal). Also decide the `SecurityFilterChain`
      layering — probably a separate chain matched on `/api/**`.
- [ ] **`GET /me` and account endpoints.** Profile, plan summary,
      feature flags, password change, email change.
- [ ] **Mailbox management endpoints.** List/connect/delete IMAP
      mailboxes, plus a two-step Gmail OAuth flow the mobile app can
      complete against its own custom URL scheme (`POST
      /mailboxes/gmail/authorize` → URL, then `POST
      /mailboxes/gmail/exchange` → refresh-token storage).
- [ ] **Chats API.** `GET /chats`, `GET /chats/{key}`, `POST
      /chats/new`, `POST /chats/{key}/send`, `POST /chats/{key}/read`.
      Response shapes must match the Thymeleaf pane's data (bubbles,
      participants, avatars, unreadCount) so the mobile chat view
      renders identically.
- [ ] **Search + saved searches API.**
- [ ] **Live delivery for mobile.** `GET /api/v1/events` SSE (or
      WebSocket) with token auth via query string, plus a
      `POST /push-subscriptions` endpoint that stores APNs / FCM
      tokens. Wire `NewMailEvent` into the push scheduler.
- [ ] **Billing endpoints for mobile.** `GET /plans`,
      `POST /billing/checkout-session`, `POST /billing/portal-session`.
      Return URLs the mobile client opens in an in-app browser.
- [ ] **Attachment download endpoint.** Blocked by "no blob storage
      yet" — see the roadmap item in `CLAUDE.md` ("Per-person shared
      files & links"). Ship blob storage first, then
      `GET /messages/{id}/attachments/{attId}`.

## Logic drift (web vs. planned API)

Nothing yet — no mobile code exists. Once endpoints ship, this list
tracks every place the Thymeleaf controller and the REST controller
call different service methods.

## Missing tests

- [ ] No test currently pins the shape of `POST /chats/refresh`
      (`{ "imported": n }`) or the SSE `new-mail` payload. Both will
      be consumed by the mobile client eventually; add response-shape
      assertions before either grows a new field.
- [ ] `ImapIdleService` has never been verified against a real IMAP
      server that supports IDLE (noted in `CLAUDE.md`); this is a
      manual verification task, not an automated test gap, but track
      it here so it doesn't get lost.

## Style / cleanup

- [ ] `web/GmailConnectController` runs a hand-rolled OAuth
      code exchange; the mobile Gmail flow will need something similar
      but token-scoped, so factor the common bits into a
      `GmailOAuthService` before duplicating.
- [ ] `docs/API.md` §2 sections are all placeholders; keep them
      honest — do not let TBDs rot when the real shapes ship.

## Roadmap items from CLAUDE.md that are blocked

Copied here so they show up in the routine's next-item picker even
though the boxes stay unchecked:

- [ ] Support email address decision (external decision).
- [ ] Google OAuth consent screen publishing (external Google Cloud
      Console action; 7-day refresh-token expiry until then).
