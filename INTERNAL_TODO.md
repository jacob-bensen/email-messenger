# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No REST API / token auth exists, so the mobile client cannot talk to the server at all. Design in docs/API.md "PROPOSED v1", then implement under `/api/v1/**` with its own stateless SecurityFilterChain (bearer token, CSRF off) and JSON error envelope.
- [ ] `/chats/refresh` requires session+CSRF; unusable from mobile until covered by the API chain.

## Logic drift / duplication risk
- [ ] `ConversationController` holds business logic (`parseRecipients`, `withSignature`, `selectMailbox`/`scopeAddress`, send orchestration: replyService + outboundMessageService + conversationKeyService). Extract into a service (e.g. `ChatSendService`) so web and API share it.
- [ ] Attachments have no bytes (`blob_ref` null) — API cannot serve downloads (see CLAUDE.md roadmap).

## Missing / spec deviations
- [ ] Generic routine assumes jOOQ; this repo uses JPA. Documented in BACKEND.md; no action unless migrating.
- [ ] Flutter-side tooling not available in the cloud env (java/mvn only) — run `./mvnw test` locally.
