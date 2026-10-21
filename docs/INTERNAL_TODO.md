# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; mobile client has nothing to call. First slice proposed in `docs/API.md` §2.
- No token-based auth (only session cookie + CSRF).

## Logic drift
- `ConversationController` holds send/mailbox-selection orchestration (signature append, sender choice, reply-vs-new). Extract into a service so a REST controller shares it.
- `ReplyService` always threads onto the last message (see CLAUDE.md roadmap: reply-to-bubble).

## Docs / tooling
- Stack differs from the generic routine assumption: JPA, not jOOQ.
- `docs/API.md` §1 SSE payload shape is TBD.
- `./mvnw`/`mvn` verify was not run during the 2026-10-21 docs bootstrap (docs-only change).

## Missing tests
- None audited yet for the future API layer.
