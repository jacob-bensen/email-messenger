# Changelog

Dated entries, newest first: what changed and why. (The pre-2026-11 history
of the earlier autonomous routine lives in `master/CHANGELOG.md`.)

## 2026-11-11 — First REST API slice for the mobile client

Why: the `conexus-mail-mobile` repo was an empty README and the backend had
no JSON API at all — every route rendered Thymeleaf behind a session cookie.
This run bootstraps the two-repo doc system and lands the smallest useful
contract: sign in and read conversations.

- **Contract**: new `docs/API.md` (canonical) covering
  `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/me`,
  `GET /api/v1/conversations`, `GET /api/v1/conversations/{key}`,
  `POST /api/v1/conversations/{key}/read`, and the error envelope.
- **Auth**: opaque bearer tokens, SHA-256-hashed at rest in the new
  `api_tokens` table (`V36__api_tokens.sql`, `ApiToken`,
  `ApiTokenRepository`, `ApiTokenService`). API logins share the website's
  `LoginThrottleService` lockout and `auth_events` audit trail.
- **Security**: second filter chain (`ApiSecurityConfig`, `@Order(1)`,
  `/api/**`): stateless, no CSRF, JSON `401`. The website chain is
  unchanged.
- **Controllers** (`com.emailmessenger.api`): `AuthApiController`,
  `ConversationApiController`, `ConversationDtos`, `ApiExceptionHandler`.
  They project the same `ConversationListService` / `ChatService` view
  models the `/chats` pages render — no business logic was duplicated.
- **Visibility**: `web.ChatService` and `auth.ClientIp` widened to `public`
  so the `api` package can reuse them.
- **Tests**: `MobileApiIntegrationTest` (13 tests) pins the contract through
  the real security chain.
- **Docs**: added `docs/BACKEND.md`, `docs/WEB.md`, `INTERNAL_TODO.md`;
  `CLAUDE.md` gained "Related repositories" and corrected stack notes.
