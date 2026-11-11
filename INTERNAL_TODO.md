# Internal TODO

Known gaps, tech debt, and deferred work, most severe first. The routine
consumes from the top and appends as it finds things. Product roadmap lives in
the mobile repo's `docs/DESIGN.md` §ROADMAP and in `CLAUDE.md` here.

## Contract breakage

- None known. (2026-11-11 audit: the six `/api/v1` endpoints, their DTOs, and
  error codes match `docs/API.md` and the mobile client's `lib/data/`.)

## Contract gaps

- **Unmatched `/api/v1/**` paths return the HTML error page.** A typo'd path
  raises `NoResourceFoundException` outside any API controller, so
  `GlobalExceptionHandler` renders `error.html` with 404. Should be the JSON
  envelope (`not_found`). Fix by making the global handler content-negotiate
  on the `/api/` prefix, or by a catch-all API controller mapping.
- **No token housekeeping.** Expired/revoked `api_tokens` rows are never
  deleted, and there is no "signed-in devices" list or "sign out everywhere"
  on `/account` (the `device_name` and `last_used_at` columns exist for it).
- **Email change does not revoke API tokens.** `AccountService.changeEmail`
  requires the current password and keeps tokens valid (they resolve by user
  id, so they keep working under the new address). Probably right, but decide
  deliberately.
- **Google-only accounts can't sign in to the app** until they set a password
  on the website. Needs an OAuth/PKCE flow for mobile.

## Logic drift (web and API must share the service layer)

- **The send flow lives in `ConversationController`** (`send`, `createMessage`,
  `sendingAccount`, `withSignature`, reply-vs-new decision, recipient parsing).
  It must be extracted into a service (e.g. `ChatSendService`) *before* a
  `POST /api/v1/conversations/{key}/messages` endpoint is added, or the API
  will fork it. This is the next mobile roadmap item.
- `ConversationController.fallbackAccount` / `accountColorMap` are view
  decisions computed in the controller; if the API ever exposes per-row
  account attribution beyond `accounts`, move them to `ConversationListService`.
- Dashboard tile counts (`ConversationListService.counts`) are not in the API;
  the app can't show unread totals/badges without them.
- The API has no live channel. The website gets `NewMailEvent`s over SSE
  (`/chats/events`, session-authenticated); the app only refreshes on pull.

## Missing tests

- No test that `api_tokens` rows cascade when a user row is deleted.
- No test for `mailbox`-scoped API calls with two connected accounts (the
  scoping itself is covered through `/chats` in `ConversationControllerTest`).
- `ApiTokenService` expiry and the `last_used_at` granularity are only covered
  indirectly; add a unit test with a fixed `Clock`.
- Live delivery part 1 (IMAP IDLE) is still unverified against a real server
  (carried over from `CLAUDE.md`).

## Style / housekeeping

- The routine prompt assumes jOOQ; this project is Spring Data JPA with no
  codegen. Docs say so; nothing to change in code.
- `master/` (BACKLOG, PLAN, MASTER_ACTIONS, a 500 KB CHANGELOG) is left over
  from an earlier routine and describes a different pricing model. Owner to
  decide whether to delete it.
- `Dockerfile` and `docker-compose.yml` are back in the repo root although
  `CLAUDE.md` records their removal on 2026-06-11.
- `README.md` doesn't mention the API or the mobile app.
