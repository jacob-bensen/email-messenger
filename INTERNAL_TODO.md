# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- No mobile REST API and no token auth exist; `docs/API.md` is a stub. Blocks any client feature.
  Suggested first feature: Bearer-token auth (`POST /api/v1/auth/token`) + `GET /api/v1/conversations`.
- CSRF + session-only security chain in `SecurityConfig`: add a stateless `/api/**` filter chain.

## Logic drift (web ↔ future API)
- `ConversationController.send` holds send orchestration (signature, sender account, reply-vs-new, recipients).
  Extract into a service (`ChatSendService`) before exposing via API.
- Controller `populateList` builds list/dashboard model inline; expose a service-level DTO for API reuse.

## Docs/stack mismatches with the generic routine
- Routine assumes jOOQ; project uses JPA. No codegen step. Keep as is unless owner decides otherwise.
- Routine says commit to `master`; this session's harness designates branch `claude/gifted-carson-ce2y1z`
  and repo CLAUDE.md forbids pushing to main. Worked on the designated branch.

## Missing tests / other
- Existing roadmap items in CLAUDE.md (Web Push, delivery states, signature card, shared files, reply-to-bubble)
  still open; attachments have no blob storage (`blob_ref` null), so the mobile client can't download them.
