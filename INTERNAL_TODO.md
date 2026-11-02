# INTERNAL_TODO

Ordered by severity (contract > logic drift > missing tests > style).

## Contract
1. No mobile JSON API: every endpoint is HTML + session cookie + CSRF. Client needs `/api/v1`
   with bearer auth and a JSON error envelope (see docs/API.md §2). Suggested first feature:
   login + `GET /api/v1/chats` + `GET /api/v1/chats/{key}` + send, delegating to existing services.
2. `GET /chats/events` SSE payload schema undocumented (TBD in API.md).
3. `/chats/refresh` returns a bare map, no error shape.

## Logic drift
4. Business logic lives partly in controllers (`ConversationController` is large: list filtering,
   mailbox resolution, send validation). Extract into services before adding an API layer so web
   and API share it.

## Docs / stack
5. Generic routine assumes jOOQ; project uses JPA. No codegen step. Keep as is.
6. DEPLOY.md/Dockerfile/docker-compose.yml exist though CLAUDE.md says Docker was removed (2026-06-11) — reconcile.
7. Open product roadmap items live in CLAUDE.md (support email, OAuth consent publish, Web Push,
   delivery states, signature cards, shared files, reply-to-bubble).

## Tests
8. Not run this session (docs-only change). Re-run `./mvnw test` before the next code change.
