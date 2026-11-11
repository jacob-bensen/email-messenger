# Changelog

One dated entry per routine run: what changed and why. (History before
2026-11-11 is in `master/CHANGELOG.md`, left over from an earlier routine.)

## 2026-11-11 — First REST API slice for the mobile client

First run of the full-stack sync routine. The mobile repo
(`conexus-mail-mobile`) was an empty Flutter placeholder and this backend had
no JSON API, so the run bootstrapped the doc system and shipped the smallest
end-to-end slice: sign in on a phone, see the chats list, read a conversation.

**Added**
- `docs/API.md` (canonical contract), `docs/BACKEND.md`, `docs/WEB.md`,
  `INTERNAL_TODO.md`, this file.
- `V36__api_tokens.sql` + `ApiToken` / `ApiTokenRepository` /
  `ApiTokenService`: opaque `cxm_` bearer tokens, SHA-256 at rest, 90-day
  expiry.
- `ApiSecurityConfig`: a second, stateless security chain for `/api/**` with
  JSON 401/403; the browser chain is unchanged.
- `com.emailmessenger.api`: `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`,
  `GET /api/v1/me`, `GET /api/v1/conversations`,
  `GET /api/v1/conversations/{key}`, `POST /api/v1/conversations/{key}/read`,
  with a single JSON error envelope (`ApiExceptionHandler`).
- Tests: `ApiAuthControllerTest`, `ApiConversationControllerTest`, `ApiTextTest`.

**Changed**
- Changing or resetting a password now also revokes the user's API tokens
  (`AccountService`, `PasswordResetService`).
- So the API and `/chats` share one code path instead of two copies:
  `ChatService` is public, takes a nullable mailbox scope, and `markRead`
  reports whether the conversation exists; mailbox-scope resolution moved out
  of `ConversationController` into `MailboxScope`; filter parsing moved to
  `ChatFilter.parse`. No behaviour change on the website.
- `ClientIp` is public so the API login can feed the shared throttle.

**Why these choices**
- Bearer tokens rather than the session cookie: the existing chain is built
  around CSRF-protected forms and login redirects, neither of which suits a
  native client.
- API logins go through the same `AuthenticationManager`, throttle, and audit
  log as form login, so there is one brute-force budget, not two.
- `GET` on a conversation does not mark it read (the web pane does); the app
  calls `POST …/read` so a prefetch can never silently clear unread state.
