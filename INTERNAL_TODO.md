# INTERNAL_TODO — email-messenger

Running list of known sync gaps and deferred work for the full-stack
sync routine. Ordered by severity (contract breakage > logic drift >
missing tests > style). Feature-launch work for the backend itself
lives in `CLAUDE.md`'s roadmap and `master/BACKLOG.md`; this file is
about cross-repo sync with `conexus-mail-mobile`.

## Contract gaps (blockers for a real mobile client)

- **No REST API exists.** Every user-facing route serves HTML. The
  `docs/API.md` matrix is entirely TBD. Nothing in the mobile repo can
  be built until the first JSON endpoint ships.
- **No token auth.** Spring Security is configured for cookie sessions
  only (form login + Google OIDC). Pick a token scheme before writing
  `/api/v1/auth/*`:
  - Option A: Spring Security Resource Server + opaque tokens stored
    server-side. Rotate on refresh. Simple to revoke.
  - Option B: JWTs signed by the backend, short-lived access + longer
    refresh. No server-side revocation list.
  Record the decision in `docs/API.md` under "Conventions" before
  implementing.
- **No mobile-safe billing flow.** Stripe Checkout works over the web,
  but the mobile client needs either a WebView redirect endpoint or a
  server-created checkout URL it can hand to the OS browser. Design
  this alongside the first billing endpoint.
- **No push infrastructure.** The backend has no VAPID keys, no
  subscription table, no send path. Roadmap item "Live delivery, part
  2" in `CLAUDE.md` covers the web-push side; mobile push (FCM/APNs)
  is a separate track that has not been designed.

## Logic-drift risks (once an API exists)

- Anything the mobile client will need to duplicate in `lib/domain/`
  (plan limits, conversation-key derivation, quote stripping,
  signature parsing) MUST be pure Java in the service layer today so
  the mirror can stay literal. Audit the top offenders when the first
  chat endpoint is designed.
- Session-based CSRF: Spring Security's default CSRF token flow does
  not fit a JSON API. The API path will need CSRF disabled and
  auth-token verification in its place.

## Missing tests

- Nothing yet; there is no REST API to test.
- When the first `/api/v1/*` endpoint lands, add a slice test
  (`@WebMvcTest`) that pins the JSON shape verbatim so the mobile side
  can't drift silently.

## Style / repo hygiene

- `master/` in the backend repo root is a leftover from the operator
  ops routine. It has its own PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS.
  Leave it alone; note in `docs/BACKEND.md` that it is separate from
  the sync-routine docs.

## Deferred (do not pick until above resolved)

- Mobile-specific rate-limiting on `/api/v1/*`.
- OpenAPI/Swagger generation for `docs/API.md`.
