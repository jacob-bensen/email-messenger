# INTERNAL_TODO (backend)

Ordered by severity.

## Contract breakage
- No `/api/**`: mobile client has no token auth and no JSON endpoints for conversations/messages/send. Blocks all mobile work. (Needs: stateless auth chain in `SecurityConfig` for `/api/**` with CSRF off + bearer tokens; DTOs; docs/API.md first.)
- `GET /chats/events` payload schema undocumented.

## Logic drift risk
- `ConversationController.send` holds send orchestration (recipients, signature, asReply, attachments). Extract to a service before exposing via REST.
- Task prompt assumes jOOQ; repo uses JPA. Treat JPA as authoritative (decision recorded 2026-10-26).
- Process conflict: routine says commit to `master`; session harness mandates branch `claude/gifted-carson-ikvf6w` (and CLAUDE.md says never push main). Branch used.

## Missing tests / docs
- Existing roadmap items in CLAUDE.md (Web Push, delivery states, per-person files with downloadable attachments) will need API surface for mobile.
- `master/CHANGELOG.md` (500KB) is the historical changelog; new routine entries go in root `CHANGELOG.md`.
