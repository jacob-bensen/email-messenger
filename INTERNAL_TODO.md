# INTERNAL_TODO — email-messenger (full-stack sync routine)

Consumed and appended to on every full-stack sync-routine run. Severity:
contract breakage > logic drift > missing tests > style.

Kept SEPARATE from `master/BACKLOG.md`, which is owned by the
backend-only autonomous routine that ships the SaaS roadmap. This file
tracks only work that keeps `../conexus-mail-mobile` in sync with the
backend.

## Contract breakage
- (none — only `/api/v1/health` is wired, and it matches `docs/API.md`.)

## Missing but planned (see docs/API.md)
- No mobile auth: implement `POST /api/v1/sessions` (opaque bearer
  tokens, symmetric-encrypted at rest like mail credentials),
  `DELETE /api/v1/sessions/current`, and `GET /api/v1/me`. Add a token
  table + a Spring Security filter that reads
  `Authorization: Bearer <token>` for `/api/v1/**` only, leaving the
  Thymeleaf session cookie chain untouched.
- No conversations API: `GET /api/v1/conversations` (paged,
  newest-first) + `GET /api/v1/conversations/{conversationKey}` +
  `POST /api/v1/conversations/{key}/messages` — all reusing
  `ConversationService` and `ReplyService` so behavior matches the
  `/chats` Thymeleaf view.
- No mobile-facing events stream: expose an SSE endpoint at
  `GET /api/v1/events` (reuse `ChatEventStream`, but auth via bearer
  token, not session cookie).

## Web ↔ API service sharing (audit item)
- Confirm every new `/api/v1/**` endpoint goes through the same service
  class already used by the Thymeleaf controller. Never inline logic
  into a new API controller.

## Tests
- `HealthApiControllerTest` covers the shape but not the security
  routing yet. Once auth lands, add an integration test that hits
  `/api/v1/*` without a session cookie and asserts it does NOT
  redirect to `/login` (the mobile client can't render an HTML login).
