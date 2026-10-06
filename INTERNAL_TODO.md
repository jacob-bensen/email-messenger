# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No mobile-facing REST API: all pages are HTML; only `/chats/refresh`, `/chats/events`,
  `/billing/webhook` return non-HTML. Client has nothing to call.
- No token auth: session cookie + CSRF only. Need `/api/v1/auth/*` (access/refresh tokens),
  with a `SecurityFilterChain` for `/api/**` (stateless, CSRF off).
- No JSON error schema (errors render `error.html`).
- Frontend repo is empty (README only); no DTOs to audit yet.

## Logic drift
- `ConversationController` is the only place that wires send/reply logic (recipient list,
  signature, new-vs-reply). Extract into a service before `/api/v1` reuses it, to avoid
  forking logic.

## Missing tests / tooling
- Quality gates not run this run (docs-only). Flutter/Dart not installed in this environment.

## Deferred
- Mobile first slice proposal: auth → chats list → open chat → send (see API.md §B).
