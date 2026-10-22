# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- [ ] No REST API exists; mobile client (`conexus-mail-mobile`) has no endpoints. Define `docs/API.md` slice (auth token, `/api/v1/chats`, `/api/v1/chats/{key}`, `POST .../send`), implement controllers over `ConversationListService`, `ChatService`, `OutboundMessageService` (do not fork logic). Needs a token auth decision (SecurityConfig is session+CSRF only).

## Logic drift
- [ ] Controllers (`ConversationController`) assemble model data (account colors, fallback account, filter parsing) inline; move to a service before an API reuses it.

## Missing tests
- [ ] `./mvnw test` baseline not run in the 2026-10-22 docs-only run (first attempt used `-o` with an empty ~/.m2 and failed to resolve the parent POM); run it online next time.

## Docs / style
- [ ] Routine spec assumes jOOQ; backend uses JPA. Docs follow the code.
- [ ] Routine says commit to `master`; this session's harness pins feature branches (`claude/gifted-carson-qduuy2`, `claude/trusting-cori-qduuy2`). Work was committed there; merge to master is left to the owner.
