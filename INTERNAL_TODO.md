# Internal TODO — backend

Running list of gaps, tech debt, and deferred work. The autonomous
routine consumes from and appends to this every run. Severity ordering:
**contract breakage > logic drift > missing tests > style**.

## Contract breakage

- **No JSON REST API for the mobile client.** Every mobile endpoint
  listed in `docs/API.md` is TBD. The only `@RestController` today is
  `BillingWebhookController`. First feature run touching mobile must
  begin by mounting `/api/v1/*` and picking a small vertical slice
  (`GET /api/v1/me` is the obvious first cut) so both sides can wire
  networking end-to-end.
- **No mobile-suitable auth scheme.** Everything is session-cookie /
  OAuth-browser-redirect based. Need device-token issuance + revocation
  before any authenticated `/api/v1/*` endpoint exists. Decide: opaque
  tokens in a new `mobile_device_token` table, vs. JWT.
- **No error envelope standard.** `GlobalExceptionHandler` returns HTML
  pages. Introduce `application/problem+json` (RFC 7807) before the
  first `/api/v1/*` handler ships.

## Logic drift risk

- Web controllers and (future) REST controllers must share the service
  layer. Currently `ConversationController`, `MailboxController`, etc.
  hold view-shaping logic (Thymeleaf model attribute assembly) alongside
  service calls; when the JSON variant lands, factor the shared parts
  into services / DTO mappers so business rules don't diverge.
- `ChatEventsController` (`/chats/events`) is SSE-specific. Mobile
  live-delivery will likely need a WebSocket or FCM/APNs bridge —
  decide, then keep both paths behind one publisher (`NewMailEvent`) so
  event definitions never fork.
- Sent-folder re-import (`lastSeenSentUid` cursor) is the current
  "Delivered" signal, but there is no outbound message state machine
  yet. When the roadmap's "Delivery states for outbound bubbles" ships
  it must expose the same states on API and web.

## Missing tests / verification

- IMAP IDLE end-to-end against a real IDLE-supporting server is still
  unverified (per roadmap in `CLAUDE.md`).
- No contract tests. Once `/api/v1/*` exists, add a fixture that
  round-trips the request/response schemas in `docs/API.md` so the doc
  can't silently rot.

## Deferred (non-blocking)

- Everything in the "Roadmap" section of the root `CLAUDE.md` — the
  routine picks the next unchecked item after this Phase 0 bootstrap.
- Google OAuth consent screen is still in Testing (weekly reconnect
  dance). Publish to production.
- Set up the support email (also in the roadmap).
