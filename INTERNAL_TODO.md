# INTERNAL_TODO

## Contract breakage (highest)
- [ ] No mobile REST API exists; `docs/API.md` §B is TBD. Needs token auth (new Flyway migration), `/api/v1` controllers reusing `ChatService`/`OutboundMessageService`, JSON error envelope. Next run's feature.
- [ ] Mobile repo `conexus-mail-mobile` has no Flutter project (README only). `flutter`/`dart` not installed in the routine environment as of 2026-11-01.

## Logic drift
- None found (no second API path yet). When the API is added, keep it on the service layer.

## Missing tests / verification
- `ImapIdleService` unverified against a real IDLE server (see CLAUDE.md roadmap).

## Process notes
- Routine says commit to `master`; this session's harness mandates branch `claude/gifted-carson-ususzu` (backend) / `claude/trusting-cori-ususzu` (mobile) and the backend CLAUDE.md forbids pushing to main. Branches used; reconcile with owner.
- Routine assumes jOOQ; backend uses JPA. Treat as JPA.
