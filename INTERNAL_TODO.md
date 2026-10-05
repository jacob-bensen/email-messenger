# Internal TODO (severity ordered)

## Contract breakage
- No JSON API or token auth exists; the mobile client cannot talk to the server. Define in
  `docs/API.md` first (auth, conversations, messages, send, mailboxes, plan limits).
## Logic drift
- None detected yet (client has no code). When the API is added, route it through the same
  services as `ConversationController` (`ChatService`, `ReplyService`, `OutboundMessageService`).
## Stack deviations from generic routine
- Backend uses JPA, not jOOQ; no codegen. Tests/`mvn verify` not yet run in this routine (see CHANGELOG).
## Missing tests / other
- `docs` assumptions about error format are TBD.
- CLAUDE.md roadmap items (support email, Google OAuth publish, Web Push, delivery states, ...) remain open.
