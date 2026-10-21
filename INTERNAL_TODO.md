# Internal TODO (backend)

Ordered by severity. Audit date 2026-10-21.

## Contract breakage
1. No mobile REST API or token auth exists; `docs/API.md` "Proposed v1" is unimplemented. The mobile repo has no code, so nothing is currently *broken*, but nothing can connect. Next feature run: `/api/v1/auth/token` + `GET /api/v1/chats` + `GET /api/v1/chats/{key}` backed by existing services.

## Logic drift
2. Task template assumes jOOQ; backend uses JPA. No action unless jOOQ is wanted.
3. Attachments are not downloadable (`blob_ref` null) — mobile will need this (see CLAUDE.md roadmap).

## Missing tests
4. Baseline `./mvnw test` result not established in the sandbox on 2026-10-21 (see CHANGELOG).

## Housekeeping
5. `DEPLOY.md`/`Dockerfile`/`docker-compose.yml` still exist though CLAUDE.md says Docker was removed.
