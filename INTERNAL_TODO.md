# Internal TODO (severity-ordered)

## Contract breakage
- No REST API / token auth for the mobile client; `docs/API.md` is a proposal only.
  Next feature run: `POST /api/v1/auth/token` + read-only `GET /api/v1/chats`, `GET /api/v1/chats/{key}`.

## Logic drift (web vs. API risk)
- `ConversationController` holds orchestration (mailbox scoping, sending-account choice,
  signature append, send routing) that must move into services before an API reuses it.
- `ChatEventStream` payload schema undocumented.

## Missing tests / tooling
- Backend `mvn -q -B compile` passed (docs-only run); tests not run.
- Flutter/Dart not installed in the environment.

## Style / docs
- Task template assumes jOOQ; this repo uses JPA.
