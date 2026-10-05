# INTERNAL_TODO (ordered by severity)

## Contract breakage
- No mobile REST API or token auth exists; the mobile repo (`conexus-mail-mobile`) is an empty README. Define
  `docs/API.md` v1 + bearer auth (`SecurityConfig` is session-only) before any client work.

## Logic drift
- Task spec assumes jOOQ; backend uses JPA. Keep JPA (CLAUDE.md conventions win).
- Web controllers (`ConversationController`, `ChatService`) hold chat view-assembly that a REST layer must reuse, not copy.

## Missing / docs
- `docs/API.md` is a gap analysis only. SSE payload shape undocumented.
- Legacy `master/` dir (PLAN/BACKLOG/500KB CHANGELOG) duplicates the CHANGELOG role; decide whether to fold into `CHANGELOG.md`.
- Frontend: Flutter project not scaffolded (`flutter create`), no `docs/DESIGN.md`/`MOBILE.md`.
- Quality gates not run this run (see CHANGELOG).

## Deferred
- Pick first feature once API v1 exists: login + chat list (read-only) across both repos.
