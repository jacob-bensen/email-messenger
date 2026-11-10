# INTERNAL_TODO

Running list of known gaps, tech debt, and deferred work. Ordered by severity:
contract breakage > logic drift > missing tests > style/docs. Consumed and
appended to by every full-stack sync run. Sibling list:
`conexus-mail-mobile/INTERNAL_TODO.md`.

Last sync audit: 2026-11-10 (first run — code vs. `docs/API.md`).

## 1. Contract

- [ ] **C1 — No mobile API exists.** The server exposes no `/api/**` JSON
      endpoints; the mobile client has nothing to call. Proposed surface is in
      `docs/API.md` §5 (0 of 7 implemented).
- [ ] **C2 — No auth a mobile client can use.** Only session cookie + CSRF.
      Unauthenticated requests get a 302 to `/login`, not a 401. Needs a
      bearer-token scheme, a separate stateless security chain for
      `/api/**`, and a decision on how Google-sign-in users (who may have
      `password_set = false`) obtain a token. Token issuance must reuse
      `LoginThrottleService` and `AuthEventService`.
- [ ] **C3 — No JSON error model.** `GlobalExceptionHandler` renders the HTML
      `error` view for everything; `/api/**` needs its own advice producing
      the error body in `docs/API.md` §5.
- [ ] **C4 — Live delivery is session-only.** `GET /chats/events` cannot be
      used by a token-authenticated client; decide SSE-with-bearer vs. push.

## 2. Logic drift / service-layer violations

- [ ] **L1 — Send logic lives in `ConversationController`.** Recipient
      parsing, from-account selection (`sendingAccount`), signature append
      (`withSignature`), reply-vs-new branching, the `(no subject)` default,
      the "body or attachment" rule, and mailbox scoping (`scopeAddress`,
      `selectMailbox`) are private controller methods. A REST send endpoint
      would have to duplicate them. Extract into a service (e.g.
      `ChatSendService`) used by both controllers **before** API endpoint 6.
- [ ] **L2 — Chat services are package-private in `web`.** `ChatService`,
      `OutboundMessageService`, and parts of `ConversationListService` are
      only reachable from `com.emailmessenger.web`; an `api` package can't
      call them without widening or moving them.
- [ ] **L3 — Send is not atomic.** SMTP send happens first, then
      `OutboundMessageService.record*` in a separate transaction. If
      recording fails the email is out but no bubble exists (until Sent-folder
      import, where that is available). Relevant to the planned delivery
      states work.
- [ ] **L4 — Outbound sender identity.** `OutboundMessageService` attributes
      recorded outbound messages to the login email's participant even when
      the mail was sent from a different connected mailbox.
- [ ] **L5 — `GET /chats/{key}` and `/pane` mutate state** (mark read). Fine
      for the browser; the API should make mark-read explicit or document it.

## 3. Web bugs found during the audit

- [ ] **W1 — Flash messages are dropped by the `/threads` bounce.** Several
      controllers still `redirect:/threads` (`GlobalExceptionHandler`
      plan-limit handler, `EmailVerificationController`, `TeamInviteController`,
      `MailboxController`, `GmailConnectController`, `AuthController`,
      `PricingController`). `ChatRedirectController` then redirects again to
      `/chats`, and flash attributes do not survive the second hop.
- [ ] **W2 — The upgrade modal is never rendered.** `upgradeModal` is set as a
      flash attribute on plan-limit hits but no template references it, so a
      user who hits a cap is silently bounced to `/chats`. Same for
      `verifyFlash` (not rendered by `chat.html`).
- [ ] **W3 — Dead reply form.** `conversation.html` (now only used by `/demo`)
      contains a form posting to `/threads/{id}/reply`, which no controller
      maps. Unreachable while `demoMode` is true; remove or repurpose.

## 4. Missing tests

- [ ] **T1** — `EndToEndMailboxFlowIntegrationTest` (Testcontainers) is
      skipped when Docker is unavailable, which includes the routine's cloud
      environment. Migrations are therefore only exercised against H2 there.
- [ ] **T2** — IMAP IDLE has not been verified against a real IDLE-capable
      server (carried over from `CLAUDE.md`).

## 5. Docs / config drift

- [ ] **D1** — `CLAUDE.md` says Docker files and CI were removed, but
      `Dockerfile`, `docker-compose.yml` and `DEPLOY.md` (which describes a CI
      image published from a `claude_routine` branch) are still in the repo.
- [ ] **D2** — `README.md` still says "MVP is being scaffolded" and lists
      attachment download links, which do not exist (`blob_ref` is always
      null).
- [ ] **D3** — `master/PLAN.md` describes an older pricing model (Free =
      1 mailbox, Personal/Team/Enterprise). Code is Free 3 / Pro 5 / Business.
- [ ] **D4** — The sync routine's prompt assumes jOOQ; this backend uses
      Spring Data JPA. Documented in `docs/BACKEND.md`; no migration planned.
- [ ] **D5** — Two changelogs: historical `master/CHANGELOG.md` and the new
      root `CHANGELOG.md` used by the sync routine. Consider consolidating.
- [ ] **D6** — The operator allowlist address is committed in
      `application.yml` (`admin.emails`); consider moving it to an env var.

## 6. Process notes

- The routine's stored prompt says to commit on `master`; the cloud session
  that ran on 2026-11-10 was restricted to a designated feature branch and
  this repo's `CLAUDE.md` says not to push to the main branch. Work from that
  run is on `claude/gifted-carson-av60mj` and needs merging by the owner.
