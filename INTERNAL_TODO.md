# INTERNAL_TODO.md
Ordered by severity (contract breakage > logic drift > missing tests > style). Updated 2026-10-10.

## Contract breakage
- No mobile REST API exists; client repo has no code. Build `/api/v1` auth (token) + chats list/read first (see API.md PLANNED).
- API auth: security chain is session+CSRF only; needs a separate stateless `/api/**` chain.
- `GET /chats/events` payload is undocumented; read `ChatEventStream` and document it.

## Logic drift
- `ConversationController` holds orchestration (`selectMailbox`, `parseRecipients`, `scopeAddress`, account colors) that a REST controller would duplicate; extract into a service before adding the API.
- Generic routine assumes jOOQ; backend uses JPA. Treat routine wording accordingly.

## Missing tests / verification
- Quality gate not run this run: `mvn test-compile` passed; full `mvn verify` not run (docs-only change).
- IDLE (roadmap) unverified against a real IMAP server.

## Process
- Routine says "commit to master"; this session's harness mandates feature branches (`claude/gifted-carson-lkb8zh`, `claude/trusting-cori-lkb8zh`). Harness rule followed; reconcile the routine text.
