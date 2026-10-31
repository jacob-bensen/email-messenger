# INTERNAL_TODO (sync audit, 2026-10-31)

Ordered by severity.

## Contract breakage
- [ ] No mobile REST API exists; `docs/API.md` has no endpoints for the Flutter client. Design token auth
      (session+CSRF is unusable from mobile) and add `/api/v1` conversations list/detail/send wrapping existing services.
- [ ] `/chats/events` SSE payload schema undocumented (inspect `ChatEventStream`/`NewMailEvent`).

## Logic drift
- [ ] Routine prompt assumes jOOQ; backend uses JPA. Treat prompt as generic; no codegen.
- [ ] Routine prompt says commit to `master`; session rules require designated `claude/*` branches. Used the latter.

## Missing tests
- [ ] No tests exist for a mobile API (none yet). Tests not run this run (see CHANGELOG).

## Deferred
- [ ] Existing roadmap items in CLAUDE.md (support email, OAuth consent publish, Web Push, delivery states, ...).
