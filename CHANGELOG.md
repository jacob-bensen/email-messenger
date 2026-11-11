# Changelog

Dated entries, newest first: what changed and why. (History before
2026-11-11 is in `master/CHANGELOG.md`.)

## 2026-11-11 — First REST API slice for the mobile client

First full-stack sync run. The backend had no JSON API and the mobile repo
was empty, so this run bootstrapped the docs and shipped the smallest
end-to-end slice: sign in on the phone and see the chats list.

- **Docs:** added `docs/API.md` (canonical contract), `docs/BACKEND.md`,
  `docs/WEB.md`, `INTERNAL_TODO.md`, this file; `CLAUDE.md` gained a
  "Related repositories" section and corrected stack notes.
- **Schema:** `V36__api_tokens.sql` — hashed bearer tokens for `/api/v1`.
- **Auth:** new stateless `/api/**` security chain with bearer-token
  authentication (`ApiTokenService`, `ApiLoginService`,
  `ApiTokenAuthenticationFilter`). API login goes through the same
  `AuthenticationManager`, audit log, and lockout as the web form. Changing
  or resetting a password on the website now revokes all of the user's API
  tokens.
- **Endpoints:** `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`,
  `GET /api/v1/me`, `GET /api/v1/conversations` (reads through the existing
  `ConversationListService`, same as `/chats`).
- **Tests:** `MobileApiIntegrationTest` (15 cases).
