# CHANGELOG

History before 2026-10-15 is in `master/CHANGELOG.md`.

## 2026-10-15 — Phase 0 bootstrap (docs + sync audit)
- Added `docs/BACKEND.md`, `docs/API.md`, `docs/WEB.md`, `INTERNAL_TODO.md`, this file; added "Related repositories"
  to `CLAUDE.md`.
- Audit: backend has no mobile JSON API; mobile repo (`conexus-mail-mobile`) is an empty scaffold. Persistence is JPA,
  not jOOQ. Business rules in `ConversationController` flagged for extraction. No code changes.
- Why: nothing to keep in sync yet; the highest-value next step is the contract-first `/api/v1` design.
- Gate: `mvn -q -B test` exit 0 (Java 21, H2; 35 migrations applied). `mvn verify` not run separately.
