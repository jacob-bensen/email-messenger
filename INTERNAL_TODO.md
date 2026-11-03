# INTERNAL_TODO (backend)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
- [ ] No mobile REST API exists; the client has nothing to call. Next feature
      run: define `/api/v1` auth (bearer tokens; session+CSRF is browser-only)
      and the read-only chats slice in `docs/API.md`, then implement on top of
      the existing services.
- [ ] No JSON error envelope; `GlobalExceptionHandler` is HTML-only.

## Logic drift
- [ ] `ConversationController.send` / `/chats/new` contain send orchestration
      (recipient resolution, signature, reply-vs-new threading, attachments).
      Extract into a service before exposing via REST so web and API share it.
- [ ] Routine template assumes jOOQ; repo uses JPA. Treat JPA as canonical.

## Missing tests
- [ ] None audited yet for API (no API). Add slice tests with each endpoint.

## Style / docs
- [ ] Historical changelog lives in `master/CHANGELOG.md`; root `CHANGELOG.md`
      is the routine's log going forward.
- [ ] Routine says work on `master`; this session's harness pins feature
      branches. Branch policy needs reconciling by the owner.
- [ ] Flutter/Dart not installed in the routine environment; frontend gates
      (`flutter analyze/test`) cannot run here.
