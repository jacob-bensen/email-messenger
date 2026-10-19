# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API or bearer-token auth exists; `docs/API.md` §2 is only a proposal. The client cannot talk to the server. (Highest value next feature: `POST /api/v1/auth/login` + `GET /api/v1/chats` + `GET /api/v1/chats/{key}`; needs a security filter chain for `/api/**` that is stateless, CSRF-exempt, and returns JSON 401.)
- SSE `/chats/events` payload shape undocumented.

## Logic drift (web vs. future API)
- `ConversationController.send` holds send orchestration (recipient list, signature append, asReply branching, recording outbound). Extract to a service before exposing via REST.
- `ConversationController.selectMailbox/scopeAddress` mailbox-scoping logic lives in the controller.

## Missing / TBD
- Stack mismatch vs. generic routine: JPA not jOOQ (documented, intentional).
- `GET /chats/events` and `POST /chats/refresh` are session-only.
- Pre-existing `master/` directory (PLAN/BACKLOG/CHANGELOG from an earlier routine) overlaps with this doc system; reconcile or fold in.
- Roadmap items in `CLAUDE.md` (Web Push, delivery states, etc.) remain open.

## Gates not run
- `mvn verify` not executed this run (docs-only change).
