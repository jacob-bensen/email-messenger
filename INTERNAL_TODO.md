# Internal TODO (backend)

Known gaps, tech debt, and deferred work. Severity ordering:
`contract > logic drift > missing tests > style`. Consume from the top;
add newly discovered items to the bottom of the appropriate section.

## Contract (blocks mobile progress)

- **Auth for `/api/**`.** Pick a scheme — plain bearer opaque token with
  a DB-backed session table, or JWT — and add the endpoints listed in
  `docs/API.md` (`/api/v1/auth/login`, `/register`, `/logout`,
  `/refresh`, `/me`). Whatever we pick, document it in
  `docs/BACKEND.md#auth-model` and mirror the storage in
  `conexus-mail-mobile/lib/data/`.
- **Chats REST parity.** `/api/v1/chats*` endpoints per `docs/API.md`
  roadmap. Reuse `ChatService` / `ConversationService` — do NOT
  duplicate the merging/labeling logic that today lives inside
  `ConversationController`.
- **SSE for mobile.** Either expose `/api/v1/events` (SSE, bearer
  auth) or move the existing `/chats/events` behind bearer auth too.
  The client needs one canonical live channel.

## Logic drift / risk

- The Full-Stack Sync Routine assumes a jOOQ + Postgres stack, but this
  project uses **JPA + Hibernate**. Do not switch to jOOQ in a routine
  run — call it out in each summary instead. (Documented in
  `docs/BACKEND.md#persistence`.)
- Actuator already exposes `/actuator/health`. `/api/v1/health` is a
  parallel surface intended for the mobile client (versioned, part of
  the contract). Do not consolidate them until we know what the client
  expects.
- CSRF is disabled for `/api/**` in `SecurityConfig`. When cookie-based
  session auth is ever considered for mobile, revisit that decision.

## Missing tests

- Integration test that runs a real Flyway migration chain against
  Postgres via Testcontainers on CI (currently local-only).
- End-to-end test hitting `/api/v1/health` from a Dart/HTTP script in
  the mobile repo, once a shared dev-mode base URL is agreed.

## Style / cleanup

- `EmailMessengerApplication.java` — verify it declares
  `@EnableScheduling` (needed by all schedulers).
- `docs/WEB.md` is a first pass; a couple of landing-page controllers
  are listed by inference from the `SecurityConfig` permit list rather
  than by grepping every controller class. Cross-check on the next
  run and correct any misclassifications.
