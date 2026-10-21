# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No `/api/v1` REST API exists; the mobile client has nothing to talk to. Define schemas in `docs/API.md`, add a
  stateless security chain + token auth, then first slice (login, chats list, chat detail, send).
- Decide auth mechanism for mobile (bearer token vs. other) — blocker for the item above.

## Logic drift
- Not yet auditable (no API). When added, controllers must reuse `ChatService`/`ReplyService`.

## Docs / process drift
- Routine says work on `master` and no PRs; this environment pins branches `claude/gifted-carson-umtun5` (backend)
  and `claude/trusting-cori-umtun5` (frontend). Work went to those branches; merging to master is left to the owner.
- Routine assumes jOOQ; backend uses JPA. No codegen step.
- Roadmap says Dockerfile/CI were removed 2026-06-11, but `Dockerfile`, `docker-compose.yml` and `DEPLOY.md` (which
  mentions CI publishing to ghcr) still exist. Reconcile.
- Existing top-level CLAUDE.md roadmap items remain the product backlog (support email, Web Push, delivery states, …).

## Missing tests / verification
- Full `./mvnw test` not run in the bootstrap run (see CHANGELOG for what was run).
