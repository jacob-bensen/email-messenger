# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style). Added 2026-10-03 (Phase 0 sync audit).

## Contract breakage
1. **No REST API / token auth for the mobile client.** Backend is session+CSRF+Thymeleaf only; `docs/API.md` §2 is a proposal.
   Next feature run: implement `GET /api/v1/chats`, `GET /api/v1/chats/{key}`, bearer-token login (new `api` package,
   `SecurityFilterChain` with `securityMatcher("/api/**")`, CSRF off, stateless), reusing `ConversationListService`/`ChatService`.
2. Mobile repo is an empty scaffold (README only) — no Flutter project, no DTOs. Needs `flutter create .`, `lib/domain/`, networking.
3. SSE event name/payload for `/chats/events` is undocumented (`ChatEventStream`).

## Logic drift (web vs. future API)
4. `ConversationController` holds logic not in a service (recipient parsing, sender-account selection, signature append,
   `selectMailbox`/`scopeAddress`). Extract into a `web`/`service` class before an API controller needs it, else it will fork.
5. `ChatService`, `OutboundMessageService` are package-private in `web`; widen/move as needed for the API package.

## Environment / tooling
6. Routine template assumes jOOQ; project uses JPA — no codegen. `CLAUDE.md` says so.
7. Branching: routine prompt says `master`; this session's harness pins `claude/gifted-carson-9ymftt` (backend) and
   `claude/trusting-cori-9ymftt` (mobile). Work was committed to those branches. Reconcile with owner.
8. `flutter`/`dart` not installed in the sandbox: frontend gates (`dart format`, `flutter analyze`, `flutter test`) can't run here.
9. Backend `mvn test` baseline not recorded this run (docs-only change).

## Style
10. Backend has legacy routine docs under `master/` (PLAN, BACKLOG, CHANGELOG 500KB) and root roadmap in `CLAUDE.md`; new
    root `CHANGELOG.md` starts at 2026-10-03 and does not replace them.
