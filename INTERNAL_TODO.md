# INTERNAL_TODO (ordered by severity)

## Contract breakage
1. No REST API / token auth exists, but the mobile repo is meant to consume one. First feature run: define `/api/v1` auth + `GET /api/v1/chats` in `docs/API.md`, then implement over `ConversationListService`/`ChatService`.

## Logic drift
2. Routine assumes jOOQ; repo uses JPA. Keep JPA (match existing architecture); routine docs updated accordingly.
3. `ReplyService` always threads onto last message (see CLAUDE.md roadmap) — API must not copy this bug.
4. Attachments have no stored bytes (`blob_ref` null) — mobile cannot download them yet.

## Missing tests
5. Not run this session: see CHANGELOG for gate results.

## Style / process
6. Routine says work on `master`; this session's harness mandated branch `claude/gifted-carson-t5rxfi`. Merge to master manually.
