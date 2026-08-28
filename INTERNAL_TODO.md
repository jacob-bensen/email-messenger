# Internal TODOs — email-messenger backend + website

Running list of known gaps, tech debt, and deferred work. Consumed and
appended to by every full-stack sync run.

Ordered roughly by severity:
1. Contract breakage (server & mobile client disagree on the wire).
2. Logic drift (business rules that live on only one side).
3. Missing tests / missing observability.
4. Style / cosmetics / nice-to-haves.

## Contract & mobile-client parity

- **Chats endpoints** are documented in `docs/API.md` §Chats but not yet
  implemented. Next milestone: `GET /api/v1/chats`,
  `GET /api/v1/chats/{key}/messages`,
  `POST /api/v1/chats/{key}/messages`. Reuse `ConversationListService`
  and `ChatService`; do not duplicate business logic in an API
  controller. The `POST` path must go through `OutboundMessageService`
  + `ReplyService` so delivery states behave the same as the web send
  path.
- **SSE stream** parallel to `/chats/events` for the mobile client, at
  `/api/v1/events`. Reuse `ChatEventStream`.
- **Mailboxes endpoints** for connect / list / delete + Gmail OAuth
  start (returns the authz URL) + reconnect. Deep-linked callback into
  the mobile app is a separate design item.
- **Token expiry.** `api_tokens` currently has no expiry — decide on a
  short-lived + refresh-token model vs. long-lived + revoke, and update
  `docs/API.md` §Auth to match. Add a scheduled sweep to purge revoked
  rows older than N days.
- **JSON error handling for framework 4xx.** The API filter chain has
  its own JSON entry point for `401` / `403`, but framework-produced
  `400` (malformed JSON, missing body) still falls through to the HTML
  `GlobalExceptionHandler`. Add a `@ControllerAdvice` scoped to
  `com.emailmessenger.web.api` so `MethodArgumentNotValidException`,
  `HttpMessageNotReadableException`, `NoResourceFoundException` all
  emit the `ApiErrorResponse` shape.

## Backend roadmap items (already in `CLAUDE.md`)

- Support email — decide the address and wire it into the app.
- Publish the Google OAuth consent screen (blocks the 7-day Gmail
  refresh-token expiry).
- Live delivery, part 2 — Web Push through the PWA service worker
  (VAPID keys, push-subscription storage, opt-in on
  `/account/notifications`).
- Delivery states for outbound bubbles (Sending / Sent / Delivered /
  Failed via DSN parsing). Mobile client mirrors the same state names.
- Signature → living contact card.
- Per-person shared files & links (requires blob storage for
  attachments).
- Reply-to-a-specific-bubble UI (Message-ID-aware threading).

## Testing gaps

- IMAP IDLE has not been verified against a real IMAP server that
  supports IDLE — do that when next running locally against a connected
  Gmail account. Noted in `CLAUDE.md` roadmap.
- No integration test yet that exercises the mobile API against a real
  Postgres via Testcontainers (currently only the H2 slice tests). Add
  one alongside `AuthFlowIntegrationTest` when we expand `/api/v1/**`.

## Observability

- Bearer-token issuance / revocation should also be recorded as
  `AuthEvent`s (new `AuthEventType.API_TOKEN_ISSUED` /
  `API_TOKEN_REVOKED`) so `/account` can list active mobile sessions
  alongside logins.

## Style / cosmetics

- The `web.api` package could grow a small `AuthenticatedUser`
  argument-resolver so controllers can accept the `User` directly
  instead of unwrapping `ApiPrincipal` themselves. Nice-to-have; add
  it when the second or third controller shows up and the pattern
  repeats.
