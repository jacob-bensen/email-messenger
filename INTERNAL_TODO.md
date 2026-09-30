# Internal TODO

Ordered by severity (contract breakage > logic drift > missing tests > style). Audit date 2026-09-30.

## Contract breakage
- No REST API and no token auth: the mobile app cannot authenticate or fetch anything. Needs a `/api/v1/**`
  stateless security chain and JSON controllers (design decision on token type first).
- `ConversationController` embeds orchestration (mailbox scoping, send new-vs-reply, recipient selection);
  extract to a service so a REST controller shares it.

## Logic drift
- Routine spec assumes jOOQ; the repo uses JPA. No codegen step exists. Routine assumptions should be updated.
- `CLAUDE.md` says Docker and GitHub Actions CI were removed (2026-06-11), but `Dockerfile`, `docker-compose.yml`
  and `.github/workflows/ci.yml` are still present. Decide: delete them or fix the note. Not touched this run.
- Routine says work on `master`; the session harness pins `claude/gifted-carson-g0o9q2`, and `CLAUDE.md` says never push to main.
  Work was committed to the session branch.

## Missing tests / verification
- Quality gates not run in the Phase 0 run (see CHANGELOG).

## Deferred
- Roadmap items in `CLAUDE.md` (support email, Web Push, delivery states, etc.).
