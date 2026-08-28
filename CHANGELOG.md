# Changelog

All notable changes to the ConexusMail backend + website are recorded
here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### 2026-08-28

- **Doc system bootstrapped** for the two-repo backend/mobile split:
  - `docs/BACKEND.md` — architecture, entities, security chains, jobs.
  - `docs/API.md` — canonical JSON contract for `/api/v1/**`.
  - `docs/WEB.md` — Thymeleaf website route inventory.
  - `INTERNAL_TODO.md` — running gaps + tech-debt log.
  - `CHANGELOG.md` — this file.
  - `CLAUDE.md` updated with a "Related repositories" section pointing at
    `jacob-bensen/conexus-mail-mobile`.
- **First JSON API endpoints for the mobile client** (contract in
  `docs/API.md` §Auth):
  - `POST /api/v1/auth/login` — email + password → `{token, user}`.
  - `GET /api/v1/auth/me` — refresh the current user.
  - `POST /api/v1/auth/logout` — revoke the bearer token.
  - New `api_tokens` table via Flyway `V36__api_tokens.sql`, storing
    SHA-256(plaintext) so a table dump can't be replayed.
  - New `com.emailmessenger.web.api` package: `ApiTokenService`,
    `ApiTokenAuthenticationFilter`, `ApiSecurityConfig` (a second
    `SecurityFilterChain` scoped to `/api/v1/**`, `@Order(1)`, stateless,
    CSRF-off, JSON error entry point), `ApiAuthController`.
  - Reuses `UserService`, `PasswordEncoder`, `LoginThrottleService`,
    `AuthEventService`, and `PlanLimitService` so throttling + audit
    logging + plan resolution stay consistent with the website.
  - Tests: `ApiAuthControllerTest` (8) + `ApiTokenServiceTest` (5) =
    13 new, full suite green (807 passed / 1 skipped / 0 failures).
