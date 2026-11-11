# Internal TODO

Known gaps, tech debt, and deferred work. Ordered by severity within each
section: contract breakage > logic drift > missing tests > style. Consume
from the top; append as things are discovered. The mobile repo keeps its
own list for client-only items.

## Contract breakage

_None known._ Server (`com.emailmessenger.api`) and client
(`conexus-mail-mobile/lib/data`) were both written against `docs/API.md` on
2026-11-11.

## Logic drift risks (web ↔ API must share services)

1. **Send/compose orchestration lives in `web/ConversationController`.**
   `send` and `createMessage` choose the sending account, append the
   signature, pick reply-vs-new-thread, call `ReplyService`, then
   `OutboundMessageService`. None of it is reachable from the API. Extract
   it into a service (and make `OutboundMessageService` usable from it)
   **before** adding `POST /api/v1/conversations/{key}/messages`, otherwise
   the API would have to fork the rules. Highest-value next feature.
2. **Mailbox scoping helpers are private statics in
   `ConversationController`** (`selectMailbox`, `scopeAddress`,
   `sendingAccount`, `fallbackAccount`, `accountColorMap`). The API therefore
   only exposes the unscoped Dashboard view. Move to a service, then add a
   `mailbox` query parameter to the conversations endpoints.
3. **Open ≠ read on the API.** The website marks a conversation read as a
   side effect of `GET /chats/{key}`; the API splits it into an explicit
   `POST …/read`. Intentional (GET stays safe), but both paths must keep
   calling `ChatService.markRead`.
4. **Website `GET` handlers trigger `refreshDueForUserAsync`**; the API
   never triggers a mailbox poll, so a mobile-only user sees mail only as
   fast as the scheduled poller / IMAP IDLE import it. Add a refresh
   endpoint or trigger the async refresh from the list endpoint.
5. **`ChatService` and `ConversationListService` live in the `web`
   package** but are now shared with `api`. Move them to `service` when
   next touching them.

6. **Page-only `@ControllerAdvice` runs on API requests.**
   `web/NavModelAdvice` and `web/SiteModelAdvice` are unscoped, so their
   `@ModelAttribute` lookups (user, mailbox list, appearance) also run for
   every `/api/v1/**` call. Wasted queries, not a correctness bug. Scope
   them to the page controllers.

## API gaps (not yet in the contract)

- Sending / replying / composing (blocked on item 1 above).
- Live updates: expose `ChatEventStream` to bearer-token clients (SSE under
  `/api/v1`), then Web/mobile push (CLAUDE.md roadmap "Live delivery,
  part 2").
- Google sign-in for mobile. Google-only accounts (`passwordSet = false`)
  cannot use `POST /auth/login` at all today.
- Registration, password reset, email verification from the app.
- Token lifecycle: no "sign out everywhere", no listing of active tokens on
  `/account`, expired/revoked `api_tokens` rows are never purged, and
  changing or resetting the password does not revoke existing tokens.
- Unknown `/api/**` paths return the website's HTML 404 instead of the JSON
  envelope (`GlobalExceptionHandler` handles `NoResourceFoundException`).
- Attachment download (bytes are not stored; `blob_ref` is always null).
- Mailbox management, billing/plan limits surfacing, saved searches, teams.

## Suspected backend bugs (found while writing docs/BACKEND.md + docs/WEB.md)

Read from source during the 2026-11-11 doc pass; **not yet reproduced** —
confirm with a test before fixing.

- **Free users at the 500-thread cap may stall polling.**
  `MailboxPollingService.pollOne` catches only `EmailImportException` /
  `ImapConnectionException`; a `PlanLimitExceededException` from
  `EmailImportService` would abort before the UID cursor is saved, so the
  same batch is refetched and fails every poll.
- **Plan-limit upgrade modal is never shown.** `GlobalExceptionHandler`
  redirects to `/threads` with flash `upgradeModal`; `/threads` redirects
  again to `/chats` and no template reads `upgradeModal`.
- **Recorded outbound Message-ID is never sent.**
  `OutboundMessageService.recordNewEmail` stores an invented
  `<uuid@conexusmail.com>` id that `ReplyService` does not put on the MIME
  message, so replies can't thread onto the recorded row by id. Outbound
  mail also goes through the app-wide SMTP relay, not the user's mailbox.
- **`UserActivityService.recordInboxVisit` has no callers**, so
  `users.last_inbox_visit_at` stays null and re-engagement keys off last
  login only.
- **Stripe price ids / several URLs have no `${ENV}` placeholder** in the
  main `application.yml` (only in the test config); `admin.emails` is
  hard-coded there.
- **Endpoints with no UI entry point**: `/billing/checkout`,
  `/verify-email/resend`, `/searches`, `/team/invite`. `conversation.html`
  (demo) still posts to the unmapped `/threads/{id}/reply`.

## Missing tests

- No Postgres run of `V36__api_tokens.sql` in this environment: the
  Testcontainers-based tests need Docker, which isn't available here, so
  the migration has only been applied to H2.
- `ApiTokenService` has no unit test for expiry (a token past `expires_at`
  is rejected) — only login/logout/revocation are covered end to end.

## Repo hygiene / stale docs

- `CLAUDE.md` says `Dockerfile` + `docker-compose.yml` were removed on
  2026-06-11, but both (and `DEPLOY.md`) are present in the tree.
- `master/` (BACKLOG, PLAN, MASTER_ACTIONS, a 500 KB CHANGELOG) belongs to
  an earlier routine and describes plans/pricing that no longer match the
  code (e.g. "MailIM", Personal/Team/Enterprise tiers vs. the actual
  `FREE`/`PRO`/`BUSINESS`). Decide whether to archive or delete.
- The routine prompt assumes jOOQ; this codebase uses Spring Data JPA /
  Hibernate with `ddl-auto: validate`. There is no codegen step.
