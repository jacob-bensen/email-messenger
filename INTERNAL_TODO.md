# Internal TODO (ordered by severity)

## Contract breakage
- No mobile REST API exists (`/api/v1/*`). Mobile client needs: token auth (current
  auth is session cookie + CSRF), JSON endpoints for conversations list/detail,
  send/reply, mailbox list/sync, plan/billing status. Define in `docs/API.md` first.
- No JSON error shape (errors render HTML via `GlobalExceptionHandler`).

## Logic drift
- Web controllers (`ConversationController`) assemble view models inline
  (list filtering/pagination in the controller). Extract into a service before
  exposing the same data via API so both share one path.

## Doc/process mismatches
- Routine assumes jOOQ; backend uses JPA. Routine says work on `master`;
  session instructions designate per-repo `claude/*` branches and CLAUDE.md says
  never push to `main` — branch instructions were followed.
- CLAUDE.md says Dockerfile/docker-compose were removed, but `Dockerfile`,
  `docker-compose.yml` and `DEPLOY.md` still exist. Reconcile (decision needed).
- `docs/BACKEND.md` schema overview is shallow; derive per-table docs from migrations.

## Missing tests / verification
- `./mvnw compile` could not run in the 2026-11-04 session (Maven could not resolve
  the project/deps offline through the proxy). Test status unverified this run.

## Roadmap items (from CLAUDE.md) still open
- Support email, Google OAuth publish, Web Push, delivery states, signature contact
  card, shared files/links, reply-to-bubble.
