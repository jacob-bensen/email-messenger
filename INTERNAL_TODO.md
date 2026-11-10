# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract / blockers for the mobile client
1. No token-based auth: all non-public routes need a session cookie + CSRF token. Add stateless `/api/**` chain (bearer/JWT or opaque tokens) and `docs/API.md` entries.
2. No JSON API for conversations, send, mailboxes. Controllers (`ConversationController`) return views; extract view-model-producing methods in services so a REST controller can reuse them.
3. No JSON error envelope; define one (code, message, field errors).
4. `/chats/events` SSE payload undocumented; document `NewMailEvent` JSON.

## Logic drift
5. `ReplyService` always threads onto the thread's last message (also in CLAUDE.md roadmap).

## Docs / tooling
6. Generic routine assumes jOOQ; project uses JPA. No codegen step. Update routine prompt if desired.
7. `README.md`/`DEPLOY.md`/`Dockerfile`/`docker-compose.yml` still present although CLAUDE.md says Docker and CI were removed — verify and prune.
8. Repo has branch `master` plus session branch `claude/gifted-carson-lwq28w`; routine said commit to master, session harness mandates the claude/* branch. Work landed on the claude/* branch.

## Missing tests
9. Docs-only run: none added.
