# INTERNAL_TODO.md (backend)

Ordered by severity.

## Contract breakage
- [ ] No mobile REST API or token auth exists; the client has nothing to call. Implement `/api/v1`
      (API.md §2) with stateless bearer auth alongside the existing session chain.
- [ ] `/chats/events` SSE payload is undocumented (API.md §1).

## Logic drift
- [ ] `ConversationController.send` holds orchestration (sender selection, signature append,
      new-vs-reply, recipient list) that a REST controller would have to duplicate. Extract into a
      service (e.g. `ChatSendService`) before adding the API.
- [ ] Routine prompt assumes jOOQ; backend uses JPA (no codegen). Docs describe JPA.

## Missing tests / gaps
- [ ] Testcontainers integration tests are skipped here (no Docker daemon).
- [ ] Attachments are not downloadable (`blob_ref` null) — needed for mobile too.

## Style / process
- [ ] Routine says commit to `master`; this session's harness pins the `claude/gifted-carson-qmgxyw`
      branch, so work landed there. Reconcile the branching policy.
