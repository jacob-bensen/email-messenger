# Internal TODO — backend

Running list of gaps, tech debt, and deferred work. Consumed and
appended by the full-stack sync routine each run. Ordered by severity:
contract breakage > logic drift > missing tests > style.

## Top-priority sync gaps (contract breakage)

- **No REST API for the mobile client.** `docs/API.md` specifies a
  `/api/v1` contract that nothing implements. Every controller today
  renders Thymeleaf HTML for a browser session. Until the API ships,
  the mobile repo has nothing to call. This is the single biggest
  blocker for the two-repo sync routine and should be the target of
  the next N feature runs.
  - First slice: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`,
    `GET /api/v1/me` behind Bearer JWTs (short-lived) + opaque refresh
    tokens stored server-side. New Flyway migration for
    `refresh_tokens` table.
  - Second slice: `GET /api/v1/mailboxes`, `GET /api/v1/chats`,
    `GET /api/v1/chats/{key}` (read-only) so the mobile app can render
    the workspace.
  - Third slice: `POST /api/v1/chats/{key}/send`,
    `POST /api/v1/chats/new`, `POST /api/v1/chats/{key}/read`.
  - Fourth slice: SSE stream at `/api/v1/events` reusing the existing
    `NewMailEvent` publisher.
  - Fifth slice: `POST /api/v1/mailboxes/imap`,
    `POST /api/v1/mailboxes/gmail/*`, `DELETE /api/v1/mailboxes/{id}`.
  - Sixth slice: `GET /api/v1/billing/plans` (public),
    `POST /api/v1/billing/checkout`, `POST /api/v1/billing/portal`.
- **No mobile client scaffolding.** `../conexus-mail-mobile` contains
  only a `README.md` and a `.gitignore`. `flutter create .` has not
  been run. See that repo's `INTERNAL_TODO.md`.

## Logic drift / operational gaps

- **CLAUDE.md tech-stack line says "no Docker".** Repo root still
  contains `Dockerfile` and `docker-compose.yml`. Reconcile: either
  update `CLAUDE.md` to acknowledge they exist as optional or delete
  the files. Track under the roadmap item that removed them.
- **`master/` directory** at the repo root carries a large legacy plan
  (`master/PLAN.md`, `master/CHANGELOG.md`, `master/BACKLOG.md`,
  `master/MASTER_ACTIONS.md`) from a previous autonomous routine.
  These are stale versus the current CLAUDE.md roadmap. Either archive
  them explicitly or delete; leaving them at the repo root is
  confusing.
- **IMAP IDLE live-delivery** flagged as "not yet verified against a
  real IMAP server" in CLAUDE.md. Still true; runs when a real Gmail
  or Fastmail account is connected.
- **Google OAuth consent screen** still in Testing mode. Refresh
  tokens expire weekly. Roadmap item still open.
- **Support email** roadmap item still open — no address decided.

## Missing tests

- No integration test yet asserts that a Thymeleaf controller and its
  future REST twin call the same service method (invariant we'll want
  to guard once both exist).
- IMAP IDLE path (`ImapIdleService`) has unit test coverage but no
  end-to-end test against a real server.
- Stripe webhook signature verification is unit-covered; add a
  Testcontainers integration test that exercises the full
  webhook-to-domain flow.

## Style / small

- Some controllers accept `Model` and `Authentication` directly and
  read the user through `UserService.byEmail(...)`. Consider a
  `@AuthenticationPrincipal` custom argument resolver that yields the
  domain `User` and shortens every controller signature.
