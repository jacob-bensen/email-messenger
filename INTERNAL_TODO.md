# INTERNAL_TODO (backend) — ordered by severity

## Contract breakage
- No JSON API / token auth / CORS: mobile client cannot talk to the server. Implement `docs/API.md` §2 (suggest first slice: `POST /api/v1/auth/token` + `GET /api/v1/chats` + `GET /api/v1/chats/{key}`).
- `/chats/events` SSE event payload undocumented; confirm shape and bearer-auth support.

## Logic drift / architecture
- `ConversationController` holds send/compose orchestration (recipient parsing, sender pick, signature, reply-vs-new). Extract to a service (e.g. `ChatSendService`) so web and API share it.
- Docs drift: `master/PLAN.md` says Free = 1 mailbox / Personal $9 / Team $29; code (`PlanLimits`) says FREE 3 mailboxes, PRO 5, BUSINESS unbounded. Reconcile.
- Two parallel planning systems (`master/*` vs CLAUDE.md roadmap); decide which is canonical.
- Task prompt assumes jOOQ; backend uses JPA — no codegen. Keep as-is unless owner decides otherwise.

## Missing tests
- Not audited this run (maven not executed). Run `./mvnw test` to baseline.

## Style / deferred
- Attachments not downloadable (`blob_ref` null) — blocks mobile attachment viewing.
- CLAUDE.md roadmap items: support email, Google OAuth publish, Web Push (mobile should use FCM/APNs later).
