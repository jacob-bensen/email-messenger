# INTERNAL_TODO — email-messenger

Running list of gaps, tech debt, and deferred work uncovered by the
autonomous full-stack sync routine. Consume from the top; add to the
bottom of the relevant section.

Severity: **contract breakage > logic drift > missing tests > style**.

## Contract breakage (client ↔ server)

Nothing shipped yet — the mobile client has only just been scaffolded.
The single live endpoint (`GET /api/version`) matches `docs/API.md`.

### Endpoints planned but not implemented (per `docs/API.md`)

Blockers for the mobile client to become useful — none is trivially
small. Pick them one at a time in future runs:

- `POST /api/auth/login` — stateless bearer-token issuance. Requires
  a new `ApiSessionToken` entity + Flyway migration + a filter that
  reads `Authorization: Bearer …` and populates the
  `SecurityContext`. Must integrate with `LoginThrottleService`.
- `POST /api/auth/logout` — token revocation.
- `GET /api/me` — profile + plan for the authenticated user.
- `GET /api/chats?page=&size=&filter=&q=&mailbox=` — lift
  `ConversationListService.list(...)` result to a stable JSON DTO
  (`ConversationListResponse` wrapping `ConversationListItemDto`).
- `GET /api/chats/{key}` — lift `ChatService.buildFor(...)` output
  (`ChatConversation` + `BubbleRun` + `BubbleMessage`) to JSON DTOs.
  Non-trivial because bubble HTML is currently rendered server-side
  into a Thymeleaf-safe form; decide whether the mobile client gets
  sanitized HTML or a structured segment list.
- `POST /api/chats/{key}/messages` — reply-in-thread + new-thread.
  Reuse `ReplyService` and `OutboundMessageService` verbatim.
- `POST /api/chats/refresh` — mirror the existing
  `ConversationController#refresh` behaviour.
- `GET /api/mailboxes` — read-only listing initially.

## Logic drift (web vs. API)

None yet — the API surface is empty. Guardrail: every future API
controller must call the same `service/` method the Thymeleaf
controller calls, with the same arguments.

## Missing tests

- No end-to-end test covers the mobile-facing bearer-token auth
  path because that path does not exist yet. Add when
  `POST /api/auth/login` is implemented.

## Web ↔ API bleed to fix before shipping any authed /api/** endpoint

- `GlobalExceptionHandler`'s catch-all `@ExceptionHandler(Exception.class)`
  currently returns the Thymeleaf `error` view with HTTP 500 for every
  uncaught exception — including `HttpRequestMethodNotSupportedException`
  and `NoHandlerFoundException` on `/api/**`. That will hand the mobile
  client an HTML error page instead of the JSON `ErrorResponse` shape
  documented in `docs/API.md`. Fix by adding a dedicated
  `@RestControllerAdvice(basePackages = "com.emailmessenger.api")` that
  emits `{ "error": "...", "message": "..." }` and by narrowing the
  Thymeleaf `@ExceptionHandler` to non-`/api/**` requests (or by scoping
  it with a `HandlerTypePredicate`). Do this in the same PR that ships
  the first authed endpoint so the client's `_errorFor` path in
  `api_client.dart` actually sees the documented slugs.

## Style / cleanup

- The `master/` directory at the repo root (`PLAN.md`, `BACKLOG.md`,
  `MASTER_ACTIONS.md`) is an older governance artefact from a
  previous routine. Left in place; do not delete without owner sign-off.
