# INTERNAL_TODO (severity-ordered)

## Contract breakage
- [ ] No mobile REST API or token auth exists; `docs/API.md` "Planned" is unimplemented. Needed before any client feature. (Also needs CSRF/session decisions for `/api/**`.)
- [ ] Frontend repo (`conexus-mail-mobile`) has only a README — no Flutter project scaffold.

## Logic drift / architecture
- [ ] `ConversationController` does mailbox scoping (`selectMailbox`, `scopeAddress`) privately; extract into a service before a JSON controller needs it, to avoid duplicating logic.
- [ ] `ChatService`/`ConversationListService` live in `web` package; consider `service`.

## Doc/process drift
- [ ] Routine prompt assumes jOOQ + `master`; repo uses JPA, and this session's harness mandates branch `claude/gifted-carson-o5x0e2`. CLAUDE.md roadmap items (support email, Google OAuth publish, Web Push, …) remain.
- [ ] Root `CLAUDE.md` says Docker was removed 2026-06-11 but `Dockerfile`, `docker-compose.yml`, `DEPLOY.md` exist — reconcile.

## Missing tests / tooling
- [ ] Backend gate (`mvn -q -B verify`) not run in the 2026-10-30 doc-only run (see CHANGELOG).
- [ ] Flutter/Dart not installed in the cloud environment; frontend gates cannot run.
