# CHANGELOG (cross-repo sync routine)

Historical product changelog: `master/CHANGELOG.md`.

## 2026-10-01
- Bootstrapped `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md` by reverse-engineering
  the code (no behavior changes). Sync audit found no REST API / token auth for the mobile client.
- Quality gate: `mvn -q -B test` exit 0.
