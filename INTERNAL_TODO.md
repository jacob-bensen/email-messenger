# INTERNAL_TODO

Rolling list of gaps, tech debt, and deferred work discovered by the
full-stack sync routine. Consumed and appended each run. Severity
order: contract breakage > logic drift > missing tests > style.

## Contract

- [ ] `POST /api/v1/session` (email/password + Google ID-token
      exchange) issuing a bearer token. Currently only session-cookie
      auth exists (browser-only). Blocking most useful mobile
      endpoints.
- [ ] `DELETE /api/v1/session` (token revocation).
- [ ] REST-specific `@ControllerAdvice` returning the JSON error
      envelope defined in `docs/API.md`. `GlobalExceptionHandler`
      currently renders HTML.
- [ ] `GET /api/v1/me` — plan, mailbox count, verified-email state.
- [ ] `GET /api/v1/chats` and `GET /api/v1/chats/{conversationKey}` —
      reuse `ConversationListService` and `ConversationService`.
- [ ] `POST /api/v1/chats/{conversationKey}/reply` — reuse
      `OutboundMessageService`; must not fork the send path.
- [ ] Mailbox connect endpoints for IMAP + Gmail OAuth, delegating to
      the same providers `MailboxController` uses on the web.

## Logic drift

- [ ] No known duplicated business logic between Thymeleaf controllers
      and the (currently minimal) REST controllers. Re-audit each time
      a new API endpoint lands.

## Missing tests

- [ ] Integration test for `/api/v1/health` and `/api/v1/version` under
      the `local` profile (not just the slice test) once a
      Testcontainers Postgres profile is stood up for API tests.

## Style / doc

- [ ] Generic routine's assumed stack lists jOOQ. This project uses
      JPA. Documented in `docs/BACKEND.md`; leave the note in place
      until the routine's assumptions are updated.
