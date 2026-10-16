# INTERNAL_TODO

Ordered by severity.

## Contract breakage
- No REST API / token auth exists; `docs/API.md` §PLANNED is unimplemented. The mobile repo
  (`conexus-mail-mobile`) is an empty scaffold (README only), so there is nothing to mismatch yet.
  Next feature run: token auth + `GET /api/v1/chats` + `GET /api/v1/chats/{key}` + send, sharing
  `ChatService`/`ReplyService`.

## Logic drift
- Routine assumes jOOQ; backend actually uses Spring Data JPA. Treat JPA as authoritative; no jOOQ codegen.
- Routine says commit to `master`; this session is restricted to branches
  `claude/gifted-carson-gq7wwb` (backend) / `claude/trusting-cori-gq7wwb` (mobile). Reconcile the schedule config.

## Missing tests / verification
- `mvn -o compile` fails offline in the sandbox (dependencies not cached); tests could not be run this session.

## Roadmap carry-over (from CLAUDE.md)
- Support email, Google OAuth consent publish, Web Push, delivery states, living contact card,
  attachment blob storage/download, reply-to-specific-bubble.
