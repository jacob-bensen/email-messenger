# Internal TODO (backend) — severity ordered

## Contract breakage
- No REST/token API exists; the mobile client has nothing to call. Needs
  `/api/v1/**` with Bearer auth (separate `SecurityFilterChain`, no CSRF,
  JSON 401s instead of `/login` redirects) and a JSON error shape.
- `GET /chats/events` payload schema undocumented.

## Logic drift
- `ConversationController` private helpers (`selectMailbox`, `scopeAddress`,
  `parseRecipients`, `chatFor`) must move into a service before a REST
  controller can share them (guardrail: no forked logic).
- Persistence is JPA, not jOOQ as the sync routine assumes; no codegen.

## Missing tests / verification
- (none open from the gate: `mvn -q -B verify` passed 2026-10-09.)
- IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).

## Style / docs
- Product roadmap lives in `CLAUDE.md` (not `docs/DESIGN.md`; that file is in
  the mobile repo).
