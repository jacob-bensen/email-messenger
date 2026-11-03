# INTERNAL_TODO

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. No REST API exists at all; `docs/API.md` is only a proposal. Mobile client cannot talk to the server.
2. No bearer-token auth; only session cookie + CSRF. Needs a separate `/api/**` security chain.
3. Attachments are not downloadable (`blob_ref` null) — no download endpoint to specify.

## Logic drift (blocks sharing web + API)
4. `ConversationController.send/createMessage` holds orchestration (sender account choice,
   signature append, `conversationKeyService.keyFor`, `sendNewEmail` + `recordNewEmail`,
   `asReply` thread choice). Extract to a service (e.g. `ChatSendService`) before adding REST.
5. `ConversationController.populateList`/`chatFor` mix model-building with query logic; extract read
   side into a service returning records.

## Spec vs reality
6. Routine spec assumes jOOQ; backend uses JPA. Treated as JPA; no codegen step.
7. Routine says commit to `master`; this session's harness mandates feature branches
   (`claude/gifted-carson-lovst2`, `claude/trusting-cori-lovst2`). Pushed there; merge to master is manual.

## Frontend
8. Mobile repo contains only a README: no pubspec, no `lib/`. Run `flutter create .` (Flutter not installed in this sandbox) and scaffold per docs/MOBILE.md.

## Missing tests / tooling
9. Full `./mvnw verify` not run this run (docs-only change; compile verified).
