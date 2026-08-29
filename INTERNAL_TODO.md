# INTERNAL_TODO

Sync-audit gaps, prerequisites for the mobile client, and cross-repo tech
debt discovered by the full-stack sync routine. Consume from and append to
this file every run. Ordered by severity: contract breakage > logic drift
> missing tests > style.

The single-repo roadmap items (product-level features) still live in
`master/PLAN.md`, `master/BACKLOG.md`, and `master/MASTER_ACTIONS.md`.
This file is scoped to the backend ↔ mobile contract.

## Sync audit findings (2026-08-29 first run)

- **Contract breakage — no REST API exists.** The mobile client cannot
  consume anything today. The plan in `docs/API.md` is entirely TBD; the
  only non-HTML endpoint is the Stripe webhook. Nothing to reconcile
  between client and server yet, but every mobile feature is blocked
  until the corresponding REST endpoint lands.
- **Logic drift risk — Thymeleaf controllers own their logic through
  services.** Confirmed by inspection that page controllers delegate to
  service classes (e.g. `ConversationController` → `ChatService` /
  `ConversationListService`, `MailboxController` → mailbox services).
  When REST endpoints are added they **must** call the same services;
  duplicating logic is a hard "no". Add a review-checklist reminder to
  `docs/API.md` once the first REST endpoint lands.

## Blockers for the mobile client (in order)

1. **Bearer-token auth for the mobile filter chain.** The existing
   Spring Security setup is cookie-session + Google OIDC + remember-me
   (JDBC tokens). It cannot authenticate a mobile client. Introduce:
   - A `mobile_sessions` table (new Flyway migration) with columns for
     an opaque token hash, user id, device fingerprint, issued-at,
     last-used-at, revoked-at.
   - A second `SecurityFilterChain` scoped to `/api/v1/**`, order
     ahead of the existing chain, that reads the `Authorization:
     Bearer …` header and resolves it to a `UsernamePasswordAuthenticationToken`.
   - Login endpoint `POST /api/v1/auth/login` that reuses
     `DbUserDetailsService` + `PasswordEncoder` and mints a token.
   - Google sign-in `POST /api/v1/auth/google` that verifies a Google
     ID token and provisions/looks up a user via `OAuth2ProvisioningService`.
   - Refresh + logout.
2. **Read endpoints for the chat workspace.** `GET /api/v1/me`,
   `GET /api/v1/mailboxes`, `GET /api/v1/chats`, `GET /api/v1/chats/{key}`.
   All must return DTOs distinct from JPA entities and share the same
   service layer as the Thymeleaf pages.
3. **Send endpoint.** `POST /api/v1/chats/{key}/messages`. Must call
   the same `OutboundMessageService` / `ReplyService` the web pane uses
   so quoting, threading, and Sent-folder syncing behave identically.
4. **Live delivery over bearer auth.** Adapt the existing
   `/chats/events` SSE (or introduce `/api/v1/events`) so it accepts a
   bearer token in the header (SSE + bearer needs the client to use
   `fetch`+`ReadableStream` since `EventSource` doesn't send headers).
   Alternative: WebSocket. Decide during design.

## Tests to add alongside the API

- Slice test per REST controller with `@WebMvcTest`.
- End-to-end test that a Thymeleaf reply and a REST reply produce the
  same `Message`, participant threading, and Sent-folder cursor.

## Style / hygiene

- Once REST DTOs land, add them as Java records under a new
  `com.emailmessenger.api` package. Follow the existing conventions:
  constructor injection only, package-private by default.
- Do **not** re-add Docker / GitHub Actions CI — the owner decision from
  2026-06-11 (recorded in the CLAUDE.md roadmap) stands.

## Deferred / open questions

- Push notifications for mobile: does the roadmap's "Web Push through
  the PWA service worker" satisfy the mobile app too via a WebView
  wrapper, or does the native app need FCM/APNs? Decide before starting
  the client push work.
