# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
1. No mobile-facing API: no `/api/**`, no token auth; all JSON-ish endpoints are session+CSRF. Client cannot be built
   until `docs/API.md` §2 is decided and implemented. (Next feature: `/api/v1/auth/login` + read-only chats.)

## Logic drift
2. Chat logic lives partly in `ConversationController` (mailbox selection `selectMailbox`/`scopeAddress`,
   recipient derivation on send). Extract into a service before an API controller duplicates it.

## Missing tests / verification
3. `docs/API.md` §1 SSE payload shape undocumented (TBD).
4. IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Process
5. Routine asks for `master` + jOOQ; this repo uses designated feature branches and JPA. Confirm with owner.
