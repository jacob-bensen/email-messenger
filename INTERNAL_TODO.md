# Internal TODO (ordered by severity)

## Contract breakage
- No `/api/v1` exists; mobile client has nothing to call. Define first slice in `docs/API.md`
  (suggest: auth + `GET /api/v1/chats` + `GET /api/v1/chats/{key}` + send) and implement over
  existing services.
- No non-cookie auth (bearer/refresh token) for mobile. Needs a design decision.
## Logic drift
- None yet (no API layer). `ConversationController` has inline user lookup + `Map` JSON for `/chats/refresh`.
## Missing tests / verification
- `./mvnw test` not run in the Phase 0 session (no local Maven repo/network verified).
## Deferred
- Attachments not downloadable (`blob_ref` null); Web Push; delivery states (see CLAUDE.md roadmap).
