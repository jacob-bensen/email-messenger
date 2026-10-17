# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- No mobile API exists; mobile repo cannot talk to the server. Decide auth model (bearer token vs. session), then implement `docs/API.md` §2.
- `bodyHtml` in `BubbleMessage` is server-rendered HTML; decide whether the mobile client renders HTML or the API exposes sanitized text/markdown.

## Logic drift / structure
- `ConversationController` holds send/compose orchestration inline (recipient parsing, sender selection, signature, conversation key). Extract to a service so web + API share it.
- Attachments aren't downloadable (`blob_ref` always null) — mobile needs a download endpoint (CLAUDE.md roadmap item).
- Routine assumed jOOQ; project uses JPA. Routine/docs should not expect jOOQ codegen.
- Repo has a stray `master/` directory (BACKLOG/PLAN/CHANGELOG from an earlier routine) alongside root docs; reconcile or remove.
- Branching: routine says commit to `master`; this session's harness mandated branch `claude/gifted-carson-8ktsp8` (repo CLAUDE.md also forbids pushing to main). Reconcile the routine config.

## Missing tests / unverified
- Quality gates not run in the 2026-10-17 docs-only run (see CHANGELOG).
- IMAP IDLE unverified against a real server (CLAUDE.md).
