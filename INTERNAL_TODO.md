# INTERNAL_TODO (ordered by severity)

## Contract breakage
1. No JSON REST API exists; mobile client has nothing to call. Implement PROPOSED `/api/v1` in
   `docs/API.md` (bearer-token auth chain in `SecurityConfig` w/ CSRF exempt for `/api/**`, then
   chats list/detail/send). Needs a decision on token format (opaque DB token vs JWT).
2. `Attachment` has no serializable DTO and `blob_ref` is null (not downloadable) — blocks mobile attachments.

## Logic drift
3. `ReplyService` always threads onto the thread's last message (see CLAUDE.md roadmap); API must
   accept an optional `inReplyToMessageId` once fixed.

## Process / docs
4. Task template assumes jOOQ; repo uses JPA. Routine's "run codegen" step is N/A.
5. Gates not run this run (docs-only): `./mvnw test` not executed.
6. Routine says commit to `master`; session harness mandates branch `claude/gifted-carson-elr3wg`
   and CLAUDE.md forbids pushing main. Followed the branch rule. (A dir named `master/` also exists
   in this repo holding PLAN/BACKLOG of a prior routine.)
