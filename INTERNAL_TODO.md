# INTERNAL_TODO.md

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- **S1** No mobile REST API or token auth exists; `conexus-mail-mobile` has nothing to call. Needs `/api/v1/**` + stateless bearer `SecurityFilterChain`, documented in `docs/API.md` first. (Suggested first feature run: auth token + `GET /api/v1/chats` list, with Flutter DTOs + login screen.)
- **S2** `GET /chats/events` payload shape undocumented.
- **S3** No JSON error envelope; `GlobalExceptionHandler` is HTML-only.

## Logic drift / structure
- **L1** Chat orchestration lives in `ConversationController` (account scoping, signature append, recipient parsing, mark-read-before-read ordering) and `ChatService`/`OutboundMessageService` are package-private in `web`. Extract to public services before building an API so web and API share one path.
- **L2** Docs mismatch: task template assumes jOOQ; repo uses JPA (documented in BACKEND.md).
- **L3** `master/` directory in repo root holds legacy planning files (500 KB `CHANGELOG.md`, `PLAN.md`, `BACKLOG.md`) from an earlier routine; PLAN.md pricing is stale vs `PlanLimits` (Free 3 mailboxes/500 threads; Pro 5). Decide whether to archive.

## Process
- **P1** Routine prompt says commit to `master`; this session's harness pins work to branch `claude/gifted-carson-v1p81p` (backend) / `claude/trusting-cori-v1p81p` (mobile) and the backend CLAUDE.md forbids pushing to main. Branch rule followed; owner to reconcile.
- **P2** `./mvnw` can't download Maven offline; use system `mvn`.
- **P3** Flutter/Dart not installed in this environment; frontend gates can't run here.

## Missing tests
- No API tests (no API). Existing suite not run this session (see CHANGELOG).
