# Internal TODO

Known gaps, tech debt and deferred work for the backend + its contract with
`conexus-mail-mobile`. Ordered by severity: contract breakage > logic drift >
missing tests > style. Consume from the top; append what you discover.

## Contract breakage

- [ ] **No mobile API exists.** `docs/API.md` §3 lists the planned slice. The
      first endpoint is blocked on three decisions recorded there: token auth
      scheme, `/api/v1` namespace, JSON error envelope. Mobile repo has no
      code, so nothing is *mismatched* yet — it is simply absent on both sides.
- [ ] **No JSON auth failure / error format.** Unauthenticated requests 302 to
      `/login` and every `GlobalExceptionHandler` branch renders HTML. An API
      needs its own security chain (401 JSON, no CSRF, no redirect) and a
      `@RestControllerAdvice` scoped to the API package.
- [ ] `new-mail` SSE event carries only `{"imported":n}` — no conversation
      keys — so a native client cannot update incrementally from it.

## Logic drift (business logic living in controllers; must move to services before an API reuses it)

- [ ] `ConversationController.send` / `createMessage`: recipient parsing,
      empty-message validation, default subject `(no subject)`, sending-account
      selection (`sendingAccount`), signature appending (`withSignature`),
      reply-vs-new decision, and the send + record pair. Extract into one
      service (send + `OutboundMessageService` record in a defined order) so
      web and API share it.
- [ ] `ConversationController`: mailbox selection/scope (`selectMailbox`,
      `scopeAddress`), `fallbackAccount`, `accountColorMap`, filter parsing.
- [ ] SMTP send and the local outbound record are two separate calls from the
      controller, not one transaction/unit — a failure between them leaves a
      sent email with no local bubble. Verify and fix when extracting.
- [ ] `MailboxController`: preference/signature parsing and validation —
      review when the mailbox API is added (not yet audited line by line).

## Bugs found during the audit

- [ ] Plan-limit upgrade modal is dead: `GlobalExceptionHandler.planLimit`
      puts `upgradeModal` in the flash map and redirects to `/threads`, but no
      template reads `upgradeModal`, and `/threads` now redirects again to
      `/chats` (flash attributes do not survive the second hop). A user who
      hits a cap is silently bounced to the chat list.
- [ ] Saved searches are orphaned in the UI: `POST /searches` and
      `/searches/{id}/delete` exist, but no user-facing template posts to them
      or lists saved searches (only the admin funnel references them).
- [ ] Stale `redirect:/threads` in `PricingController`, `TeamInviteController`
      and `GlobalExceptionHandler` — works via `ChatRedirectController` but
      costs a hop; point them at `/chats`.

## Docs / repo hygiene

- [ ] Stack mismatch with the routine's assumption: persistence is JPA, not
      jOOQ. Documented in `docs/BACKEND.md`; no migration planned.
- [ ] `master/` directory (`PLAN.md`, `BACKLOG.md`, `MASTER_ACTIONS.md`, a
      500 KB `CHANGELOG.md`) is a stale planning system from an earlier
      routine: wrong product name (MailIM), wrong plans/prices, references to
      `/threads`. Owner to decide: delete or archive. New changelog entries go
      in the root `CHANGELOG.md`.
- [ ] `CLAUDE.md` says Docker files and CI were removed, but `Dockerfile`,
      `docker-compose.yml` and `DEPLOY.md` (which describes a CI-published
      image on a `claude_routine` branch) are in the tree. Owner to reconcile.
- [ ] `README.md` not re-verified against the current product in this audit.
- [ ] `admin.emails` in `application.yml` hardcodes a personal address;
      consider moving to an env var.

## Missing tests

- [ ] No test asserts the JSON shape of `POST /chats/refresh` beyond what
      `ConversationControllerTest` covers — TBD, verify when the API lands.
