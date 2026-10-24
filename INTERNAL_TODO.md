# INTERNAL_TODO

Ordered by severity (contract breakage > logic drift > missing tests > style).

## Contract breakage
- No REST/JSON API exists; the mobile client (`conexus-mail-mobile`) has nothing to call.
  First feature run: bearer-token auth + `GET /api/v1/chats` (+ detail) per `docs/API.md`.
- No non-session auth (token issuance, `/api/**` security chain, JSON 401s).

## Logic drift
- `ConversationController` holds business logic (mailbox scoping, sending-account choice,
  attachment conversion, `populateChat`). Extract to services so web and API share it.
- Mobile `lib/domain/` does not exist yet; plan limits/conversationKey rules must be mirrored
  once it does.

## Missing tests / verification
- IDLE live delivery unverified against a real IMAP server (see CLAUDE.md roadmap).
- Full `./mvnw test` not run during the 2026-10-24 docs-only run (see CHANGELOG).

## Style / housekeeping
- A `master/` directory sits at the repo root (appears to be stray); investigate before removing.
- Root `Dockerfile`/`docker-compose.yml`/`DEPLOY.md` exist although CLAUDE.md says Docker was removed.
