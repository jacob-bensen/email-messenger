# Internal TODO

Known gaps, tech debt, and deferred work, ordered by severity
(contract breakage > logic drift > missing tests > style). Consumed and
appended to by every full-stack sync run. Product roadmap lives in
`CLAUDE.md`; human-only actions live in `master/MASTER_ACTIONS.md`.

Last sync audit: 2026-11-11.

## Contract breakage

1. **No REST API exists for the mobile client.** Every controller returns
   HTML or a redirect. `docs/API.md` §2 is a proposal only. Build it in this
   order, one slice per run: token auth → `GET /api/v1/me` →
   `GET /api/v1/conversations` → timeline → send.
2. **No auth a native client can use.** `SecurityConfig` has one
   session-cookie + CSRF chain. Needs a second, stateless chain for `/api/**`
   with bearer tokens. Token model is undecided (opaque DB-stored token vs.
   JWT, lifetime, revocation, how Google-only accounts with no password sign
   in from mobile). Decide and record in `docs/API.md` before implementing.
3. **No JSON error contract.** `GlobalExceptionHandler` renders the HTML error
   page for every exception and redirects on `PlanLimitExceededException`.
   `/api/**` needs its own `@RestControllerAdvice` producing the error body in
   `docs/API.md`, and unauthenticated API calls must return 401 rather than a
   302 to `/login`.
4. **The mobile repo has no code.** `conexus-mail-mobile` holds only docs, so
   there is no client call site to audit yet.

## Logic drift

5. **Chat business logic lives in `ConversationController`**, not a service:
   `sendingAccount`, `withSignature`, `parseRecipients`, `scopeAddress`,
   `selectMailbox`, and the reply-vs-new-email orchestration in `send` and
   `createMessage`. An API controller would have to copy it. Extract into a
   service (for example `ChatSendService`) used by both, before adding the
   send endpoints.
6. **SMTP send and outbound record are not one transaction.** `send` calls
   `replyService.send…` then `outboundMessageService.record…` from the
   controller; a failure between them leaves a sent email with no bubble.
   Fold into the extracted service.
7. **Several controllers read repositories directly**: `DigestController`
   (`DigestEmailPreferenceRepository`), `MailboxController`
   (`MailAccountRepository`), `AccountController` (`SubscriptionRepository`),
   `TeamInviteController` (`TeamInviteRepository`). Route through services
   before exposing the same features over the API.
8. **Services needed by the API are package-private in `web`**
   (`ChatService`, `OutboundMessageService`, and others). Decide whether API
   controllers live in `web` or whether these move to `service`.
9. **Timestamps are `LocalDateTime` assumed UTC** (`toInstant(ZoneOffset.UTC)`
   in the view records). The API must emit epoch millis, never the raw
   `LocalDateTime`.
10. **`PlanLimitExceededException` redirects to `/threads`**, a legacy URL
    that bounces to `/chats`. Works, but the flash attribute must survive two
    redirects — verify the upgrade modal still shows.

## Stale documentation

11. `master/PLAN.md` and `master/MASTER_ACTIONS.md` describe old pricing
    (Personal / Team / Enterprise, $9 / $29 / $99) and the name "MailIM". The
    code has `FREE` / `PRO` / `BUSINESS` and "ConexusMail".
12. `CLAUDE.md` says the Dockerfile and docker-compose were removed, but
    `Dockerfile`, `docker-compose.yml`, and a Docker-based `DEPLOY.md` are in
    the repo. Either delete them or correct the note — owner decision.
13. `README.md` not audited this run.

## Missing tests

14. No test pins the JSON shape of `POST /chats/refresh` or the SSE payload
    as a contract (they are covered functionally in `ConversationControllerTest`
    and `ChatEventStreamTest` — confirm and tighten when the API reuses them).

## Process

15. The sync routine is written for `master`, while the session harness
    assigns a per-run `claude/*` branch and this repo's `CLAUDE.md` says not
    to push to the default branch. Until the owner reconciles these, each
    run's work lands on its session branch and must be merged before the next
    run can build on it.
