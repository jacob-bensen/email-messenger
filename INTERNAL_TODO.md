# Internal TODO (ordered by severity)

## Contract breakage
- [ ] No REST API or token auth exists; mobile client cannot talk to the server. Design `/api/v1` (auth, conversations, send) in `docs/API.md` first.
## Logic drift
- [ ] Controllers (e.g. `ConversationController`) return view models for Thymeleaf; extract JSON-able DTOs from the same services (`ChatService`, `ConversationService`, `ReplyService`) when adding the API — do not fork logic.
- [ ] Task brief assumed jOOQ; project uses JPA. Treat JPA as authoritative.
## Missing tests / verification
- [ ] Live IDLE not verified against a real IMAP server (see CLAUDE.md roadmap).
## Process
- [ ] Brief says commit to `master`; session rules require the designated `claude/*` branch and CLAUDE.md forbids pushing to main. Work happens on the branch; merge is a human decision.
