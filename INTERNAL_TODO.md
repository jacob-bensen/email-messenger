# Internal TODO (ordered by severity)

## Contract breakage
- No REST API / token auth exists; mobile client (`conexus-mail-mobile`) has no code and nothing to call. Blocks all client work.

## Logic drift / structure
- `ConversationController.send` orchestrates sending (recipients, signature, reply-vs-new, persistence) inline; extract to a service before exposing via REST so web and API share it.
- `ConversationController.populateList`/account-scope helpers (`selectMailbox`, `scopeAddress`) are controller-private; needed by any API.
- Task prompt assumes jOOQ; the project uses JPA. Treat JPA as the convention.

## Missing tests
- No contract tests for `/chats/refresh` JSON shape.

## Docs / process
- Task prompt says commit to `master`; this session is restricted to feature branches (`claude/gifted-carson-coff1e`, `claude/trusting-cori-coff1e`), so work was committed there.
- CLAUDE.md roadmap items (support email, OAuth publish, Web Push, etc.) remain open.
