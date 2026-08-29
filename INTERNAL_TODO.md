# INTERNAL_TODO — email-messenger (backend)

Running list of gaps, tech debt, and deferred work. Ordered by severity:
contract breakage > logic drift > missing tests > style. Consumed and
appended to on every full-stack sync run.

Reciprocal list on the mobile side: `conexus-mail-mobile/INTERNAL_TODO.md`.

## Severity: CONTRACT

- **No REST API for the mobile client exists yet.** As of 2026-08-29
  the backend exposes zero endpoints intended for the Flutter client.
  `docs/API.md` documents the planned shape (bearer-token auth, JSON,
  `/api/v1/*`) but nothing is implemented. First implementation
  milestone should be `POST /api/v1/auth/login` + `GET /api/v1/me`
  (Spring Security config split so `/api/**` uses stateless
  bearer-token auth alongside the existing session-based Thymeleaf
  auth). Blocks every mobile feature.

- **Refresh-token storage table not designed.** Once the auth
  endpoint lands, refresh tokens need a `refresh_tokens` table
  (id, user_id, hashed_token, issued_at, expires_at, revoked_at,
  last_used_at) with the standard rotation-on-use pattern. Add a
  Flyway migration in the same run as the login endpoint.

- **Google OAuth ID-token verification for mobile is not wired.**
  The current `OAuth2ProvisioningService` and `GoogleOidcUserService`
  assume the server did the OAuth handshake itself; the mobile app
  will do the handshake and hand over an ID token instead. Extract a
  reusable "verify Google ID token → provision or fetch User"
  method so both paths converge on one service call.

- **SSE stream `/chats/events` is session-cookie authenticated.**
  When the mobile SSE equivalent (`/api/v1/chats/stream`) lands, it
  must accept `Authorization: Bearer …` (SSE clients in Dart use
  `http` or `eventsource_client`, both of which pass headers).

## Severity: LOGIC DRIFT

- **Business rules to mirror on the mobile side (`lib/domain/`).**
  The mobile client should mirror, not re-derive: participant-set
  `conversationKey` computation (`ConversationKeyService`), the IM
  transform (`IMTransformService` — quoted-reply strip, sender
  collapse into `BubbleRun`), the reply-threading rule
  (`ReplyService` currently always threads onto the thread's last
  message — track the roadmap follow-up), and the plan-limit table
  (`PlanLimitService` / `PlanLimits`). Any behavior that ships on
  both sides must have a matching pure-Dart implementation with the
  same test vectors.

- **`ReplyService` mis-threads replies to older bubbles** (already
  called out in `CLAUDE.md` roadmap: "Reply-to-a-specific-bubble").
  Do not implement mobile reply-to-bubble UI until this is fixed on
  the server, otherwise the mobile UX will visibly break threading.

## Severity: MISSING TESTS / OBSERVABILITY

- **No test coverage exists for API DTO stability**, because the API
  does not exist yet. Once endpoints land, add a `web/api` slice test
  (`@WebMvcTest`) per endpoint asserting the exact JSON shape (field
  names + null-vs-absent semantics) — a serialization regression on
  the wire is a mobile crash.

- **No JSON schema published for third parties.** Not urgent until
  post-v1, but if we ever open the API, a `springdoc-openapi`
  dependency + `/api/v1/openapi.json` would let the Flutter client
  codegen its DTOs instead of hand-writing them.

## Severity: DOCS / OPS

- **`master/` docs and root `docs/` overlap.** `master/PLAN.md` /
  `BACKLOG.md` / `CHANGELOG.md` / `MASTER_ACTIONS.md` predate the
  full-stack-sync convention and remain the source of truth for the
  backend's own roadmap. The root `docs/` + `CHANGELOG.md` +
  `INTERNAL_TODO.md` (this file) are the sync-routine files that
  bridge to the mobile repo. Keep both up to date; do not delete
  the `master/` set — the backend's per-run routine still writes
  there.

- **`CLAUDE.md` still lists a "target" package layout** even though
  the packages exist. Not misleading, but worth pruning next time
  someone touches that file. Low priority.

- **Mobile-facing "how to authenticate against the API" doc** is
  missing from `docs/`. Add a `docs/AUTH_FLOWS.md` when the auth
  endpoints ship.
