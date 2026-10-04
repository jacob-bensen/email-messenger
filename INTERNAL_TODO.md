# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No REST API / token auth exists for the mobile client; mobile repo is an empty shell. Define DTOs in `docs/API.md`, add `/api/v1` controllers over existing services (`ConversationListService`, `ChatService`, `ReplyService`), add bearer auth to `SecurityConfig`.

## Logic drift
- Web controllers (`ConversationController`) assemble view models inline (scope/mailbox selection helpers); extract into a service before a second (API) consumer duplicates it.

## Missing tests
- None audited this run.

## Style / docs
- Task spec assumes jOOQ; this repo uses JPA. Treat spec's jOOQ steps as N/A.
- Changelog lives at `master/CHANGELOG.md` (a stray `master/` directory holds BACKLOG/PLAN/CHANGELOG); consider moving to root.
- Roadmap items in `CLAUDE.md` remain the product backlog.
