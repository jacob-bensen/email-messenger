# Internal TODO (gaps, debt, deferred)

Ordered by severity: contract breakage > logic drift > missing tests > style.

## Contract breakage
1. **No REST API for the mobile client.** Only session+CSRF HTML endpoints
   exist (plus `/chats/refresh`, `/chats/events`). Build `/api/v1` per
   `docs/API.md` §2, starting with read-only `GET /api/v1/chats` and
   `GET /api/v1/chats/{key}`.
2. **No token auth.** Need a stateless bearer-token chain (separate
   `SecurityFilterChain` for `/api/**`, CSRF off) and `POST /api/v1/auth/token`.
   Account lockout (`LoginThrottleService`) must apply to it too.

## Logic drift (web/API sharing)
3. `ConversationController` holds send orchestration (sender account choice,
   signature append, `ReplyService` + `OutboundMessageService`, reply-vs-new
   decision) and mailbox-scope selection (`selectMailbox`, `scopeAddress`,
   `fallbackAccount`, `accountColorMap`). Extract into a service so REST reuses
   it without forking logic.
4. Frontend `lib/domain/` mirror of `PlanLimits` (3/5/unlimited mailboxes,
   500-thread free cap) and `ChatFilter` semantics is not started.

## Missing / unverified
5. Attachment bytes are not stored (`blob_ref` null) — mobile cannot download.
6. IMAP IDLE unverified against a real server (see CLAUDE.md roadmap).
7. Mobile push (FCM/APNs) absent; Web Push is roadmap item "part 2".

## Process notes
- Routine prompt says "work on master"; this environment designates feature
  branches (`claude/gifted-carson-hbnjmu`, `claude/trusting-cori-hbnjmu`) and
  repo CLAUDE.md forbids pushing to main. Branch instructions followed.
- Routine assumes jOOQ; backend uses JPA.
