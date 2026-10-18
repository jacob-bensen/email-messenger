# INTERNAL_TODO

Ordered by severity.

## Contract breakage
- No JSON API or bearer-token auth exists; the mobile client cannot talk to the server. Define `/api/v1` in docs/API.md, add token auth to `SecurityConfig`, then implement login + chats list/detail + send. (Chosen as the next feature.)

## Logic drift
- Audit whether web controllers (`ConversationController`, `MailboxController`) hold logic that should live in services before a REST layer reuses it (not done in Phase 0).
- Attachments are not downloadable (`blob_ref` null); the API would have to expose this honestly.

## Missing tests / tooling
- jOOQ is not used (JPA); generic routine's codegen step does not apply.
- `./mvnw -q -B test-compile` passes; full `verify` not run in Phase 0.
- Tracked `master/` directory holds a prior routine's BACKLOG/CHANGELOG/PLAN/MASTER_ACTIONS (EPIC-style history); reconcile with the new docs/CHANGELOG.md or retire one of them.

## Style
- CLAUDE.md says "Target"/"planned" for long-built things; refresh opportunistically.
