# INTERNAL_TODO (ordered by severity)

## Contract breakage
1. No `/api/v1` JSON API and no bearer/token auth — the mobile client has nothing to call. (API.md has the proposed contract.)
2. Send/compose orchestration lives in `ConversationController` (sendingAccount, withSignature, parseRecipients, recordReply/recordNewEmail); extract to a service so API + web share it (the `/chats/new` and `/chats/{key}/send` paths also duplicate it).

## Logic drift
3. Task template assumes jOOQ; backend uses JPA. Treat JPA as authoritative; no codegen.
4. `master/PLAN.md` describes Personal/Team/Enterprise pricing; code has FREE/PRO/BUSINESS (`PlanLimits`: FREE 3 mailboxes/500 threads). Reconcile or retire `master/`.
5. Plan caps must be mirrored in the client's `lib/domain/` once it exists.

## Missing tests / verification
6. Not yet verified: IMAP IDLE against a real server (see CLAUDE.md roadmap).
7. Quality gates not run in the discovery run (see CHANGELOG).

## Process
8. Routine says "work on master"; the cloud session is pinned to `claude/*` branches and CLAUDE.md forbids pushing main. Branch-pinned instructions were followed.
