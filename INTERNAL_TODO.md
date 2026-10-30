# INTERNAL_TODO (backend)

## Contract breakage
1. No REST API / token auth for the mobile client (`docs/API.md` §2). Needs `/api/**` security chain (stateless, bearer),
   CSRF exemption, JSON error envelope, DTOs over existing services. Suggested first feature: `POST /api/v1/auth/token`
   + `GET /api/v1/chats`.
2. `/chats/events` SSE payload undocumented.

## Logic drift risk
3. Mailbox scope selection and send-path handling live in `ConversationController`; extract into a service before a
   REST controller reuses them.

## Stack deviations from the routine template
4. Persistence is JPA, not jOOQ; no codegen step. Routine's backend gate is `./mvnw -q -B verify`.

## Missing tests
5. No tests exist for any JSON endpoint contract (`/chats/refresh`).

## Housekeeping
6. `master/` directory holds the older routine's BACKLOG/CHANGELOG/PLAN/MASTER_ACTIONS; root `CHANGELOG.md` is the
   new sync-routine log. Consider consolidating.
7. Roadmap items in `CLAUDE.md` (support email, Google OAuth publish, Web Push, delivery states, …) remain open.
8. This sync routine's "work on master" instruction conflicts with the session's required feature branch
   (`claude/gifted-carson-jpzmd5`) and CLAUDE.md ("do not push to main"); the session branch was used.
