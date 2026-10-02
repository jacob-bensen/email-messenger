# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No mobile API exists; `docs/API.md` "Planned" section is a design target only.
  Needs: token auth (CSRF-exempt, stateless chain at `/api/**`), JSON error
  envelope, first slice (list chats, open chat, send).
- `/chats/events` SSE payload shape is undocumented (read `NewMailEvent` /
  `ChatEventStream` and document).

## Logic drift
- `ConversationController.send` holds send orchestration (reply-vs-new,
  signature append, recipient derivation, sending-account choice). Extract into
  a service so web and API share one path.
- Roadmap in `CLAUDE.md` still has open product items (support email, Google
  OAuth publish, Web Push, delivery states, attachment storage).

## Missing tests
- None audited this run.

## Style / process
- Routine prompt assumes jOOQ; backend uses JPA. Routine prompt says commit to
  `master`; this session environment mandates feature branches
  (`claude/gifted-carson-2uvc8p`, `claude/trusting-cori-2uvc8p`). Docs were
  committed on the branch.
