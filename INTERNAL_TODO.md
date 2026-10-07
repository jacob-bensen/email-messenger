# Internal TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No `/api/v1` exists; mobile client has nothing to call. Implement `docs/API.md` §2.1–2.3.
- [ ] No token auth: `SecurityConfig` has a single session+CSRF filter chain. Add a second
      `SecurityFilterChain` (`/api/**`, stateless bearer) ahead of the web chain.

## Logic drift (web vs future API)
- [ ] `ConversationController` holds orchestration (mailbox scoping, send/reply branching,
      signature append, attachment conversion, `refresh`). Extract into a service so API
      and web share it before adding API controllers.

## Doc/stack discrepancies
- [ ] The routine template assumes jOOQ; the repo uses JPA. Do not add jOOQ; template note only.
- [ ] `docs/BACKEND.md` flag inventory is TBD.

## Missing tests / quality gates
- [ ] `./mvnw verify` baseline not recorded (2026-10-07: `mvn test` passes).
