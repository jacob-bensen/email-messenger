# Internal TODO — Backend

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract / cross-repo (with `conexus-mail-mobile`)

- **No mobile-facing API yet** beyond `GET /api/v1/health`. Every screen
  on `conexus-mail-mobile/docs/DESIGN.md` § ROADMAP needs matching
  endpoints under `/api/v1/**` — auth (session), me, conversations list,
  conversation view, reply, live SSE, mailbox connect, push registration.
- Auth model is session-cookie based. Fine for a first-party mobile
  client but revisit before shipping publicly — token auth (opaque
  bearer or JWT) is friendlier to mobile lifecycles and easier to revoke
  server-side.
- `HealthApiController.API_VERSION` is hardcoded. Wire it to
  `spring-boot-maven-plugin`'s `build-info` goal + `BuildProperties` so
  it stays honest across releases.
- No shared error-envelope helper yet. The `{"error", "message", "fields"}`
  shape documented in `docs/API.md` needs a `@ControllerAdvice` in
  `web/api/` (distinct from the existing `GlobalExceptionHandler`, which
  is Thymeleaf-oriented) before the first mutating endpoint lands.

## Test / build gates

- Quality gate not run: `mvn -q -B verify` was NOT executed this run.
  Rerun locally before merging.
- No CI. The team removed the GitHub Actions workflow (see roadmap in
  `CLAUDE.md`); tests run locally with `./mvnw test`. Do not re-add CI.

## Style / cleanup

- Consider extracting the growing `permitAll` list in
  `SecurityConfig#securityFilterChain` into a named constant array once
  the `/api/v1/**` prefix grows.
- The `web/` package now mixes MVC controllers, view models, and
  service classes (`ChatService`, `ConversationListService`,
  `OutboundMessageService`, …). The REST controllers live in
  `web/api/`; keep them there and consider moving the services down to
  `com.emailmessenger.service` in a future cleanup.

## Backend roadmap (from `CLAUDE.md`)

Not restated here — see `CLAUDE.md` § Roadmap. This file only tracks
work discovered by the full-stack sync routine (cross-repo mismatches,
tech debt, deferred cleanups).
