# INTERNAL_TODO (severity-ordered)

## Contract breakage (blocks mobile)
1. No JSON API exists; `docs/API.md` v1 is planned only. Implement `/api/v1` auth + chats list/detail/send on the existing services.
2. No token auth (bearer/refresh) — add stateless `SecurityFilterChain` for `/api/**` (CSRF off there).
3. `docs/API.md` DTOs `ChatDetail`/`ChatBubble`/`Mailbox` unspecified.

## Logic drift (web controller holds logic that an API would need)
4. `ConversationController`: `withSignature`, `parseRecipients`, `selectMailbox`, send/compose flow → extract to services before API reuse.

## Repo hygiene
5. `.github/workflows/ci.yml`, `Dockerfile`, `docker-compose.yml`, `.dockerignore` are tracked although CLAUDE.md says they were removed 2026-06-11. Owner decision needed; not touched.
6. Routine prompt assumes jOOQ + `master` branch; repo uses JPA, and this session is pinned to branch `claude/gifted-carson-ccd5qz`.
7. `master/` directory (PLAN/BACKLOG/CHANGELOG ~500KB) overlaps with CLAUDE.md roadmap; consolidate.

## Missing tests
8. No controller-level JSON tests (no JSON controllers). Attachments not downloadable (`blob_ref` null) — mobile will need it.
