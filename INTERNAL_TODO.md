# Internal TODO (ordered by severity)

## Contract breakage
- [ ] No REST API exists, but the mobile client needs one. Design `/api/v1` + token auth in `docs/API.md`, then implement on top of the existing services (not by forking controller logic). (Nothing in `conexus-mail-mobile` consumes the API yet, so this is a gap, not a live break.)

## Logic drift / architecture
- [ ] Verify `ConversationController` has no logic that should live in a service before the REST API reuses it (it builds recipients, picks sender account and signature inline: `parseRecipients`, `sendingAccount`, `withSignature`). Extract to a service when the API needs it.
- [ ] Spec assumes jOOQ; the project uses JPA. Routine prompt stack assumption is wrong for this repo; follow the code.

## Missing tests
- [ ] Not audited this run (118 test files exist; `./mvnw test` was not run, see CHANGELOG).

## Style / docs
- [ ] `docs/BACKEND.md` package table lists some services by name without confirming their package; verify.
- [ ] Routine says "master only"; the environment assigns `claude/*` branches. Reconcile with owner.
