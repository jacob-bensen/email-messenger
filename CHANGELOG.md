# Changelog

Dated entries, newest first: what changed and why. (The much older
`master/CHANGELOG.md` is a historical log from a previous automation and is
no longer appended to.)

## 2026-11-11 — REST API foundation for the mobile client

First full-stack sync run. The backend had no JSON API and the mobile repo
was empty, so this run bootstrapped the doc system and shipped the smallest
end-to-end slice: sign in on mobile and see the chats list.

**Added**

- `docs/API.md` (canonical contract), `docs/BACKEND.md`, `docs/WEB.md`,
  `INTERNAL_TODO.md`, this file; "Related repositories" in `CLAUDE.md`.
- `V36__api_tokens.sql` + `ApiToken` / `ApiTokenRepository` /
  `ApiTokenService`: opaque bearer tokens for `/api/**`, stored as SHA-256,
  90-day expiry, revocable.
- `ApiLoginService`: password login for the API that reuses the website's
  login throttle and `auth_events` audit rows, so web and API failures share
  one lockout budget.
- A second, stateless `SecurityFilterChain` for `/api/**` (bearer auth, no
  CSRF, JSON 401 instead of a login redirect). The website chain is
  unchanged.
- `com.emailmessenger.api`: `POST /api/v1/auth/login`,
  `POST /api/v1/auth/logout`, `GET /api/v1/me`, `GET /api/v1/conversations`,
  JSON error envelope, JSON 404 for unknown `/api/**` paths.

**Changed**

- Mailbox-scope resolution and `filter` parsing moved out of
  `ConversationController` into `web.MailboxScope` and `ChatFilter.parse`, so
  `/chats` and `/api/v1/conversations` cannot drift. No behavior change on
  the website.
- Changing or resetting a password now revokes all of that user's API tokens
  (`AccountService.changePassword`, `PasswordResetService.consumeToken`).

**Verified**

- `mvn -q -B verify`: 822 tests, 0 failures, 0 errors, 1 skipped
  (`EndToEndMailboxFlowIntegrationTest` needs Docker).
- Booted against a local PostgreSQL 16: V36 applied cleanly; login, `/me`,
  conversations, logout, and the error cases exercised with curl.
