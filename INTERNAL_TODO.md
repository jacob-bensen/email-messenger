# Internal TODO (backend)

Running list of known gaps, tech debt, and deferred work. Consumed and
appended to on every full-stack sync run. Ordered by severity:
**contract breakage > logic drift > missing tests > style**.

## Contract breakage

- **No REST API for the mobile client.** The whole `docs/API.md` surface
  is unimplemented — only Thymeleaf HTML endpoints and the Stripe webhook
  exist today. Bootstrap `com.emailmessenger.api` with `@RestController`s
  under `/api/v1/**`, delegating to existing `service/` classes. First
  slice: `POST /api/v1/auth/login`, `GET /api/v1/me`, `GET /api/v1/chats`,
  `GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/send`.

- **No bearer-token auth scheme.** Current auth is cookie-session only.
  The mobile client cannot use cookies cleanly; introduce an opaque
  token store (`user_api_tokens` table with hashed token, expiresAt,
  lastUsedAt) and a `TokenAuthenticationFilter` sitting alongside the
  existing form/OIDC login on `/api/v1/**` only (leave `/chats` etc.
  cookie-based). CSRF must be disabled on `/api/v1/**`.

- **SSE `/chats/events` is cookie-scoped.** Once bearer tokens exist,
  expose `/api/v1/events` accepting the same header.

## Logic drift risk

- Business logic in controllers must move to `service/` before the REST
  layer lands, so both callers share it. Audit needed:
  `ConversationController#send`, `MailboxController#create`,
  `AuthController#register`.
- `OutboundMessageService` state transitions (Sending / Sent / Delivered /
  Failed) need to be observable by API polling or SSE — today the web
  view relies on template re-render.

## Missing / weak tests

- No integration test asserts service layers can be composed without a
  Spring web context (needed to prove REST controllers can reuse them).
- Add Testcontainers-backed test for the (future) `TokenAuthenticationFilter`.

## Roadmap alignment

Existing backend roadmap in `CLAUDE.md` unchanged. Two new items to fold
into it once REST work starts:

- Bootstrap `/api/v1` REST layer (bearer tokens, auth, /me, chats).
- Publish REST-facing DTOs as Java records mirroring `docs/API.md`.

## Cross-repo

- Backend + mobile share `docs/API.md`; changes must land in the same
  run in both repos.
- Mobile repo needs `flutter create .` to be run to add platform folders
  (android/, ios/) — no Flutter tooling was available in this environment
  to do it here.

## Style / hygiene

- Roadmap in `CLAUDE.md` mixes web-only items with cross-cutting ones;
  once REST work begins, split them or annotate.
