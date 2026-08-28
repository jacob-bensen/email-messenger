# INTERNAL_TODO

Gaps, tech debt, and deferred work discovered by the sync routine.
Ordered by severity: contract breakage > logic drift > missing tests >
style. Consume from the top; add newly discovered items with a date.

## Contract breakage / missing surface

- **No mobile REST API exists.** The only `@RestController` in the
  backend is `BillingWebhookController` (Stripe). Every mobile-facing
  endpoint documented in `docs/API.md` is a target shape, not
  implemented code. Next runs should add endpoints one vertical slice
  at a time, starting with `GET /api/v1/health` (trivial) and the
  auth pair (`POST /auth/login`, `POST /auth/refresh`,
  `GET /me`) so the mobile client has something to talk to.
  Added: 2026-08-28.

- **Auth model split.** The web app uses form login + session cookies
  + CSRF; the mobile client needs bearer tokens. Requires:
  - A `Bearer` token filter mounted only under `/api/**`.
  - A `refresh_tokens` table (opaque token, `user_id`, `expires_at`,
    `revoked_at`) via a new Flyway migration.
  - CSRF disabled for `/api/**` in `SecurityConfig`.
  - `LoginThrottleFilter` behavior extended to the API path.
  Added: 2026-08-28.

- **Gmail OAuth on mobile.** `docs/API.md` sketches
  `POST /api/v1/mailboxes/gmail/start` returning a browser auth URL;
  the redirect completion needs to reach the *authenticated mobile
  user*, not the current web-session user. Design deferred until the
  bearer-auth layer lands. Added: 2026-08-28.

## Logic drift

- **`conversationKey` is server-computed.** Client MUST NOT construct
  it. Document this in the mobile networking layer once it exists so
  no one is tempted to reproduce `ConversationKeyService` in Dart.
  Added: 2026-08-28.

- **Attachment downloads.** `Attachment.blob_ref` is unused today —
  attachments aren't downloadable. Any mobile-side "shared files" view
  must wait on the roadmap "Per-person shared files & links" item in
  `CLAUDE.md`. Added: 2026-08-28.

## Missing tests

- No integration test exists for the (still-to-be-added) `/api/v1/**`
  path family. When the first endpoint lands, add a
  `WebMvcTest`-level slice plus a full-stack Testcontainers test that
  drives auth-through-chat-fetch end to end. Added: 2026-08-28.

## Style / cleanups

- `docs/BACKEND.md` says "eight" specifics without measuring them —
  when adding features, keep it in sync (package layout, migration
  count, feature-flag names). Added: 2026-08-28.
