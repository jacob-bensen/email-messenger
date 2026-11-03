# Internal TODO (sync audit, 2026-11-03)

Ordered by severity.

## Contract breakage
1. No REST/JSON API exists; the mobile client has nothing to call. Needs bearer-token auth (current model: session cookie + CSRF), `/api/v1/**` controllers, and promotion of `docs/API.md` §2.
2. Auth redirects unauthenticated requests to `/login` (302 HTML) rather than 401 — unsuitable for API clients; add a separate `/api/**` filter chain.

## Logic drift / shared service layer
3. `ConversationController` holds orchestration in private methods (`parseRecipients`, `withSignature`, `sendingAccount`, `selectMailbox`, `scopeAddress`, send+record sequence). Extract into a service (e.g. `ChatSendService`) before adding REST so web and API share it.
4. `ChatService`, `OutboundMessageService` and other web-package services are package-private in `web`; widen/move as needed for API controllers.
5. `BubbleMessage.attachments` exposes the JPA `Attachment` entity — needs a DTO before JSON serialization.

## Docs / drift
6. CLAUDE.md roadmap says Dockerfile/docker-compose and CI were removed, but `Dockerfile`, `docker-compose.yml`, and `DEPLOY.md` (references ghcr/CI) still exist.
7. SSE event payload shape (`ChatEventStream`) not documented — TBD.
8. Routine prompt assumes jOOQ + `master`; reality is JPA, and platform requires `claude/gifted-carson-nhuymm`. Update the stored routine prompt.

## Missing tests
9. No tests for any `/api/**` (n/a until built).
