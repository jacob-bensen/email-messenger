# Internal TODO (ordered by severity)

## Contract breakage
1. No mobile REST API exists (`docs/API.md` §2 is PLANNED). The client has nothing to call. Needs a decision on mobile auth (bearer token vs session+CSRF; Google sign-in on mobile) — all current auth is cookie + CSRF.
2. Realtime for mobile: `/chats/events` SSE is cookie-authed; push (FCM/APNs) not designed (roadmap "Web Push" is web-only).

## Logic drift / architecture
3. `ConversationController` holds orchestration (recipient parsing, `selectMailbox`, `sendingAccount`, `withSignature`, key computation + send + record). Extract into a service so web and `/api/v1` share it.
4. Generic target says jOOQ; actual is JPA. Docs/CLAUDE.md now say so; no action unless a migration is wanted.
5. Attachments aren't stored (`blob_ref` null) — mobile can't download them (roadmap item in CLAUDE.md).
6. Outbound delivery states + reply-to-specific-bubble (roadmap) will change message DTOs; fold into the contract when built.

## Missing tests
7. No controller tests will exist for `/api/v1` until it is built.

## Environment
8. Quality gates (`./mvnw verify`) not run during the docs-only bootstrap run.
