# INTERNAL_TODO

## Sync audit (2026-10-21), by severity
1. **Contract breakage – no mobile API exists.** Backend exposes no `/api/**`; auth is session+CSRF only. Mobile repo (`conexus-mail-mobile`) has only a README (no Flutter project). Next feature run: define `/api/v1` auth + chat list/detail in `docs/API.md`, implement, then scaffold Flutter.
2. **Logic drift risk.** Core chat services (`ChatService`, `ConversationListService`) and `ConversationController` are package-private in `web/`; an API package must reuse them (widen visibility or move) rather than copy logic. `parseRecipients`, `selectMailbox`, signature-append logic live in the controller and should move to a service before the API reuses them.
3. **Stack mismatch with routine assumptions.** Backend uses JPA, not jOOQ; no codegen step.
4. **Doc drift.** `CLAUDE.md` says Docker/CI were removed, yet `Dockerfile`/`docker-compose.yml` exist; `master/` holds an older routine's PLAN/BACKLOG.
5. Errors are HTML-only (`GlobalExceptionHandler`); need JSON envelope for `/api/**`.
6. Attachments not downloadable (`blob_ref` null) – blocks mobile attachment viewing.
