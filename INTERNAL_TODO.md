# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- No REST API / token auth for the mobile client exists; `docs/API.md` §2 is only a plan. Blocks every mobile feature.
- Frontend repo `conexus-mail-mobile` is empty (README only); no Flutter project, so no client↔server mismatch can exist yet.

## Logic drift
- Controllers (`ConversationController`) assemble view models inline (`populateList`, `chatFor`, scope selection, filter parsing); extract to a service before an API duplicates it.
- Target-stack mismatch: routine assumes jOOQ; repo uses JPA. Docs follow the code.

## Missing tests / verification
- Quality gates this run: `mvn test-compile` only; full `mvn verify` not run (doc-only change).
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Deferred
- Attachments not downloadable (`blob_ref` null) — needed for mobile too.
