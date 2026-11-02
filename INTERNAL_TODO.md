# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- No mobile REST API or token auth exists; `docs/API.md` has nothing to sync against. Needs a spec (first feature: `/api/v1/auth/token`, `/api/v1/conversations`, `/api/v1/conversations/{key}/messages`, reply). Decide token scheme (JWT vs opaque) first.
- `GET /chats/events` SSE event name/payload undocumented — read `ChatEventStream` and fill `docs/API.md`.
- No JSON error envelope; errors are HTML.

## Logic drift
- None found: web controllers go through services. Re-audit when the REST API lands.

## Tech debt / process
- Routine assumes jOOQ + `master`; repo uses JPA and the session mandates branch `claude/gifted-carson-o08uu1`. Routine pushes go to the session branch, not master.
- `.github/workflows/ci.yml` exists although CLAUDE.md says CI was removed (2026-06-11). Confirm and delete or fix the note.
- Existing roadmap/changelog live in `master/` (PLAN.md, BACKLOG.md, CHANGELOG.md ~500KB, MASTER_ACTIONS.md). Root `CHANGELOG.md` is new for this routine; consider consolidating.
- Attachments not stored (`blob_ref` null) — mobile can't download files until the blob-store roadmap item lands.

## Missing tests
- Not run this session: full `./mvnw test` (docs-only change).
