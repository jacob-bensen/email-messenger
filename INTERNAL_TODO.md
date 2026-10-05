# INTERNAL_TODO (severity-ordered)

## Contract breakage
1. **No REST API for the mobile client.** Only HTML/session routes exist (`docs/WEB.md`). Define `/api/v1/**` in `docs/API.md` first, then implement over existing services.
2. **No token auth.** Session cookie + CSRF only; add a stateless bearer chain for `/api/**` (decide scheme; handle Google-only users with no password).
3. `GET /chats/events` SSE payload shape undocumented — read `ChatEventStream`/`NewMailEvent` and document.

## Logic drift
4. Controllers are thin, but `ConversationController` does recipient parsing (`parseRecipients`), mailbox scoping (`selectMailbox`, `scopeAddress`) and attachment checks inline. Extract into a service before an API reuses it, to avoid forked logic.

## Missing / unverified
5. Attachments not downloadable (`blob_ref` null) — blocks a mobile attachment UI (see CLAUDE.md roadmap).
6. Quality gates not run this session (docs-only change): `mvn` is installed, `./mvnw test` not executed.
7. Roadmap items still open: support email, Google OAuth consent publish, Web Push (needed for mobile notifications), delivery states, reply-to-bubble.

## Style / process
8. Routine assumes jOOQ + `master`; repo uses JPA and session branches — recorded in `docs/BACKEND.md` and `CLAUDE.md`.
