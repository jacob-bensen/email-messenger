# Changelog

Newest first. Each entry is one autonomous-routine run.

## 2026-08-29 — Full-stack sync bootstrap + first mobile-facing API

- Phase 0 doc bootstrap: added `docs/BACKEND.md`, `docs/API.md`,
  `docs/WEB.md`, `CHANGELOG.md`, `INTERNAL_TODO.md`. Updated `CLAUDE.md`
  with a "Related repositories" section pointing at
  `jacob-bensen/conexus-mail-mobile`.
- New package `com.emailmessenger.api` with `ApiChatsController`
  exposing `GET /api/v1/chats` (paginated JSON, filter/mailbox/q
  parameters). Delegates to the existing `ConversationListService` — no
  duplicate query path. Returns a paginated envelope of `ChatSummary`
  DTOs.
- `SecurityConfig`: `/api/**` requires auth (redirect on 401 for now)
  and is CSRF-exempt (only GET endpoints are exposed until bearer-token
  auth ships — see INTERNAL_TODO.md).
- Added `ApiChatsControllerTest` (Spring MVC slice, mocked service).
