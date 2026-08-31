# Changelog (backend)

Human-readable, rolling. Newest first. One entry per sync-routine run.

## 2026-08-31

- **API bootstrap.** Added the first REST endpoint, `GET /api/v1/health`,
  under a new `com.emailmessenger.api` package (`ApiHealthController`,
  `HealthResponse`). Returns `{status, apiVersion, serverTime}` and is
  reachable unauthenticated. Test in
  `src/test/java/com/emailmessenger/api/ApiHealthControllerTest.java`.
- **Security.** Permitted `/api/v1/health` in `SecurityConfig`; exempted
  `/api/**` from CSRF (mobile clients are stateless and carry their own
  auth token — session cookies do not apply). No other `/api/**` route
  exists yet, so no route becomes accessible by widening CSRF exemption.
- **Docs.** Bootstrapped `docs/API.md` (canonical REST contract),
  `docs/BACKEND.md` (architecture), `docs/WEB.md` (Thymeleaf inventory).
  Added a "Related repositories" section to `CLAUDE.md` naming the
  sibling `conexus-mail-mobile` repo and each doc's role. Created
  `INTERNAL_TODO.md`.
