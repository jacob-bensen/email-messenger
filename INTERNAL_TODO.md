# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No versioned JSON API exists; mobile repo has nothing to call. Implement
      `/api/v1` (planned in `docs/API.md` §2) as thin controllers over
      `ChatService` / `ConversationListService` / `OutboundMessageService`.
- [ ] No token auth: session+CSRF only. Design bearer/refresh-token auth
      (new Flyway migration for token storage) before any mobile endpoint.
- [ ] `/chats/events` SSE payload shape undocumented (TBD in API.md).

## Logic drift / duplication risks
- [ ] `ConversationController` holds mailbox-selection and signature logic
      (`selectMailbox`, `scopeAddress`, `withSignature`) as private statics.
      Extract into a service so a REST controller reuses it instead of copying.
- [ ] Persistence is JPA, not jOOQ as the sync routine assumes; the routine's
      jOOQ-codegen steps do not apply.

## Missing tests / verification
- [ ] `mvn verify` not run during the 2026-09-30 bootstrap (docs-only run).

## Deferred
- [ ] Repo also has `master/` (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS) from a
      separate routine; root `CHANGELOG.md` is the sync routine's log.
- [ ] Routine says commit to `master`; this session's harness pins the branch
      `claude/gifted-carson-70pvno`. Work went there, no PR opened.
