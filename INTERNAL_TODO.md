# INTERNAL_TODO (backend)

Ordered by severity. Created 2026-10-21 from the Phase 0 sync audit.

## Contract breakage
- [ ] **No REST API for the mobile client.** The frontend repo (`conexus-mail-mobile`) is an empty
      scaffold; `docs/API.md` lists the PLANNED `/api/v1` surface only. Needs: bearer-token auth
      (new filter chain under `/api/**`, CSRF-exempt, stateless), DTO records, controllers calling
      existing services. First feature to pick up.
- [ ] `GET /chats/events` payload shape undocumented (read `ChatEventStream`).

## Logic drift / structure
- [ ] `ConversationController` holds orchestration (mailbox selection, scope address, list
      population) — extract into a service so API controllers share it, not copy it.
- [ ] Routine prompt assumes jOOQ; project uses JPA. Docs follow the code.
- [ ] Repo has a stray `master/` directory (PLAN/BACKLOG/CHANGELOG/MASTER_ACTIONS) from an earlier
      routine; root `CHANGELOG.md` now exists for this routine. Consider consolidating.

## Missing tests
- [ ] None audited yet for the (nonexistent) API.

## Deferred (from CLAUDE.md roadmap)
Support email, Google OAuth publish, Web Push, delivery states, signature contact card,
attachment blobs, reply-to-bubble.
