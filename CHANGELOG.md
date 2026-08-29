# Changelog

Ongoing log of what changed and why. Roadmap-driven work also updates
`CLAUDE.md` (backend roadmap) and, for cross-repo features,
`../conexus-mail-mobile/docs/DESIGN.md`.

## 2026-08-29

- Bootstrapped the doc scaffold required by the full-stack sync routine:
  `docs/BACKEND.md`, `docs/WEB.md`, `docs/API.md`, this `CHANGELOG.md`,
  and `INTERNAL_TODO.md`.
- Updated `CLAUDE.md` with a "Related repositories" section pointing at
  the sibling `conexus-mail-mobile` Flutter client.
- Landed the first `/api/v1` REST endpoint — `GET /api/v1/health` — as
  the shared contract for the mobile client's launch-time reach check
  (`HealthApiController` + `HealthApiControllerTest`). Permitted the
  route unauthenticated and exempted `/api/v1/**` from CSRF in
  `SecurityConfig` so future mutating JSON endpoints from the mobile
  client can post without a token (session cookie still required for
  anything authenticated).
- Quality gates: `mvn -q -B verify` — NOT run in this environment (see
  INTERNAL_TODO.md); the change is a single new controller + test and a
  small tweak to the existing security filter chain.
