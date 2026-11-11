# Internal TODO

Known gaps, tech debt, and deferred work, most severe first within each
section. Consume from the top; remove items when done; add what you find.
Severity order: contract breakage > logic drift > missing tests > style.

Last sync audit: 2026-11-11.

## Contract breakage

None. Every endpoint in `docs/API.md` is implemented by the server and
called by the client with matching shapes (checked 2026-11-11).

## Logic drift (rules that live where the API can't reach them)

1. **Send / reply / compose logic is inside `web.ConversationController`.**
   Private methods decide the sending account (`sendingAccount`), append the
   signature (`withSignature`), parse recipients (`parseRecipients`), pick
   reply-vs-new, and derive recipients from conversation members. A send
   endpoint would have to copy them. Extract a `ChatSendService` (single
   transaction: validate → send → record) used by the controller *before*
   adding `POST /api/v1/conversations/{key}/messages`.
2. **`web.ChatService` is package-private and returns view models that embed
   JPA entities** (`BubbleMessage.attachments` is `List<Attachment>`). The
   conversation-detail endpoint needs it public and needs DTO mapping that
   doesn't leak entities.
3. **Opening a conversation marks it read as a side effect of a GET**
   (`ConversationController.chat` / `chatPane`). Fine for a page; for the API,
   specify an explicit mark-read call so prefetching doesn't clear unread
   state.
4. **`ConversationController.fallbackAccount` / `accountColorMap`** compute
   per-row account attribution and colors for the website's Dashboard. Not
   exposed by the API (`accounts` is returned raw). Move to a service if
   mobile needs the same fallback attribution.
5. **`GlobalExceptionHandler.planLimit` redirects to `/threads`**, a legacy
   URL that only works via `ChatRedirectController`. The API has no mapping
   for `PlanLimitExceededException` yet (it would surface as
   `500 internal_error`) — add a `402`/`409`-style code when the first
   plan-limited endpoint (connect mailbox) is added.

## Missing API surface (website can, mobile can't)

Ordered by value to a mobile user. Each needs `docs/API.md` first.

1. Conversation detail: timeline, members, signatures; mark read.
2. Send message / reply (after logic-drift item 1); attachments upload.
3. Live updates: SSE works only with the session cookie today
   (`/chats/events`); mobile needs a bearer-authenticated stream and then
   Web/FCM push (roadmap "Live delivery, part 2").
4. Registration + email verification from the app.
5. Google sign-in for the API (Google-only accounts currently cannot use the
   app until they set a password on the website).
6. Mailbox management (connect IMAP / Gmail OAuth, sync now, remove,
   signature, default sender).
7. Saved searches; account settings; billing status (purchases on mobile are
   an app-store policy question — decide before building).

## Security / robustness

1. **`ClientIp` trusts `X-Forwarded-For` unconditionally** (pre-existing).
   Without a proxy that overwrites the header, a caller can rotate it to
   dodge the per-IP login throttle (per-email still applies). Make trust
   configurable / restrict to known proxies.
2. No rate limiting on authenticated API calls; only login is throttled.
3. Expired and revoked `api_tokens` rows are never deleted. Add a small
   scheduled purge.
4. No "signed-in devices" list on `/account` to view or revoke individual API
   tokens (`device_name` and `last_used_at` are already recorded for it).
5. `ApiLoginService` stamps `last_login_at` for API logins, so mobile-only
   users count as active for the re-engagement sweep — intended; but
   `last_inbox_visit_at` is not updated by `GET /api/v1/conversations`.
   Decide whether mobile list views should count as inbox visits.

## Missing tests

1. `EndToEndMailboxFlowIntegrationTest` (Testcontainers) is skipped wherever
   Docker isn't available, so no automated test runs the migrations on real
   PostgreSQL in those environments. V36 was checked by hand on PG16.
2. No test asserts `mailboxId` scoping through the API with two mailboxes
   (the shared `MailboxScope` is covered only indirectly by web tests).
3. No contract test shared between the repos; DTO parity is enforced by
   hand-written fixtures on each side (`ConversationApiControllerTest` here,
   `test/fixtures.dart` in mobile).

## Stale docs / housekeeping

1. `README.md` still says "in development … MVP is being scaffolded" and
   doesn't mention accounts, billing, or the API.
2. `CLAUDE.md` roadmap says `Dockerfile` / `docker-compose.yml` were removed
   2026-06-11, but both files and `DEPLOY.md` (Docker-based) are in the repo.
   Decide which is true.
3. Branch policy is contradictory: `CLAUDE.md` "Working agreement" says to
   stay on the session's branch and never push to `main` (the default branch
   is actually `master`), while the full-stack routine's stored prompt says
   to commit straight to `master`. The 2026-11-11 run followed the session
   branch. Owner to pick one and update both places.
4. `master/` directory of legacy planning files (500 KB changelog) sits at
   the repo root and shadows the branch name in git commands
   (`git log master` is ambiguous). Consider deleting or moving to `docs/archive/`.
5. Package layout in `CLAUDE.md` is labelled "target"; it is now actual and
   omits `auth`, `billing`, `admin`, `team`, `digest`, etc. (`docs/BACKEND.md`
   has the full list).
