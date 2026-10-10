# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. **No mobile API / token auth.** Only cookie-session + CSRF HTML endpoints exist; mobile has zero
   callable endpoints. Next run: implement `/api/v1/auth/*` (stateless bearer, separate filter chain
   for `/api/**`) + `GET /api/v1/chats`, `GET /api/v1/chats/{key}` over `ConversationService`/`ChatService`;
   promote from API.md §B to §A in the same run as the Flutter client.
2. `GET /chats/events` SSE payload and JSON error envelope undocumented (API.md says TBD).

## Logic drift
3. Persistence is JPA, not jOOQ as the routine template assumes; keep as is, documented in BACKEND.md.
4. Sync audit of web vs service layer was a spot check only; do a full pass when /api/v1 is added.

## Missing tests / style
5. Attachments are not downloadable (`blob_ref` null) — blocks mobile attachment support (see CLAUDE.md roadmap).
6. Existing roadmap items in CLAUDE.md (support email, Google OAuth publish, Web Push, …) remain open.
