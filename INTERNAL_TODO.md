# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style). Last audit: 2026-10-30.

## Contract breakage
- No REST/JSON API for the mobile client exists; `docs/API.md` §A lists the only JSON/SSE endpoints. Client (`conexus-mail-mobile`) is an empty README, so nothing is *broken*, but the first feature must define `/api/v1` (API.md §B).
- No token auth: only session cookie + CSRF. Mobile needs bearer/token auth (and a story for Google sign-in) before any `/api/v1` endpoint.
- No JSON error envelope; errors are HTML via `GlobalExceptionHandler`.

## Logic drift (web vs API share-service rule)
- `ConversationController.send` (`/chats/{key}/send`) holds recipient resolution, sending-account choice, signature appending and thread selection inline. Extract to a service (e.g. `ChatService`/`OutboundMessageService`) before an API endpoint reuses it.
- `ConversationController.populateList` assembles list model inline; reuse `ConversationListService` from an API controller rather than copying.

## Deviations from the routine's assumptions
- Persistence is Spring Data JPA, not jOOQ; no codegen step.
- Routine says commit to `master`; this session was pinned to feature branches (`claude/gifted-carson-ftmxor`, `claude/trusting-cori-ftmxor`), so work was committed there. Repo CLAUDE.md also forbids pushing to main.

## Missing tests / tooling
- `./mvnw` cannot download its distribution in the cloud sandbox (wget to repo.maven.apache.org fails); use system `mvn` there.
- Test baseline for this audit: see CHANGELOG.md.

## Deferred
- Roadmap items in CLAUDE.md (support email, Google OAuth publish, Web Push, delivery states, living contact card, shared files, reply-to-bubble).
- Attachment bytes are not stored (`blob_ref` null) — mobile cannot download attachments until the files roadmap item lands.
