# Internal TODO

Known gaps, tech debt and deferred work, most severe first. The sync routine
consumes from the top and appends what it discovers. Product roadmap lives in
`conexus-mail-mobile/docs/DESIGN.md` §ROADMAP (mobile) and `CLAUDE.md`
(website).

## 1. Contract breakage (client ↔ server)

- [ ] **No mobile API exists.** No `/api/**` endpoints, no token auth, no
      JSON error envelope, no CORS. The mobile app cannot sign in or load
      anything. First slice: API security chain + bearer-token login +
      `me` (see `docs/API.md` §5 for the open decisions).
- [ ] **Chat services are not reachable from an API controller.**
      `ChatService` and `OutboundMessageService` are package-private in
      `web/`, and the send flow (recipient parsing, sending-account choice,
      signature append, reply-vs-new, default subject) is private code in
      `ConversationController`. Extract a public send/conversation service
      and have the web controller call it, so web and API share one path.
- [ ] **View models leak JPA entities.** `BubbleMessage.attachments` is
      `List<Attachment>` and `Conversation.thread` is an `EmailThread`;
      the API needs DTO records.
- [ ] **Timestamps are zone-less.** `LocalDateTime` values are UTC by
      convention only; the API must emit explicit UTC instants.
- [ ] **Live events carry only a count** (`{"imported":N}`), so any client
      must refetch the whole list.

## 2. Logic drift and correctness

- [ ] **Sent messages can appear twice.** `recordReply` stores the local
      copy with no Message-ID and `recordNewEmail` with a synthetic one, so
      the Sent-folder import cannot de-duplicate against either.
- [ ] **Conversation keys go stale** when the owner connects or removes a
      mailbox: existing threads are not re-keyed (the startup backfill only
      fills null keys).
- [ ] **Outbound mail uses the app SMTP relay with the user's From
      address** — no per-mailbox SMTP or Gmail send. Deliverability
      (SPF/DKIM/DMARC) risk; plain text only.
- [ ] **Plan is set at checkout start, not on payment**; webhooks never
      change `Subscription.plan`. No Stripe event-id de-duplication.
- [ ] **Flash messages lost on `/threads`.** Redirects to `/threads` bounce
      again to `/chats` and drop flashes, so the plan-limit upgrade modal,
      `verifyFlash` and `teamFlash` never render. Several flashes have no
      template reading them at all.
- [ ] **Business rules in controllers** (beyond the send flow): sync
      frequency / plan floor and signature length in `MailboxController`;
      the Gmail OAuth state machine in `GmailConnectController`; opt-out
      mutation in `DigestController`; password length in
      `PasswordResetController`; duplicated plan→checkout logic in
      `AuthController` and `PlanCheckoutSuccessHandler`.
- [ ] **Time zones.** Entity `@PrePersist` stamps and
      `MailAccount.markSynced()` use the JVM default zone while `Clock` and
      `sent_at` are UTC; correct only when the JVM runs in UTC.
- [ ] `participants` is global across users; display name is first-writer-
      wins, so one user's sender name shows for another.
- [ ] `Message.messageIdHeader` is annotated `unique = true` but V3 dropped
      the constraint; `findByMessageIdHeaderAndOwner` returns `Optional` and
      would throw on duplicates.

## 3. Security and operations

- [ ] `CredentialEncryptor` falls back to a hardcoded dev key (WARN only)
      when `MAILBOX_ENCRYPTION_*` are unset; production should refuse to
      start instead.
- [ ] `admin.emails` is hardcoded in the committed `application.yml`.
- [ ] `ClientIp` trusts the leftmost `X-Forwarded-For` value (spoofable
      without a trusted proxy). No rate limit on register, forgot-password,
      send or `/chats/refresh`. No Content-Security-Policy.
- [ ] `ChatEventStream` is in-memory: single instance only.
- [ ] Search is `LIKE '%q%'` with no full-text index; a conversation loads
      all its messages with no pagination.

## 4. Missing tests

- [ ] Only one test exercises real PostgreSQL, and it needs Docker; the
      large JPQL conversation queries are otherwise checked on H2 only.
- [ ] The cron `run*Cycle()` jobs call their `@Transactional send*For`
      methods on `this` — the same self-invocation shape as the polling bug
      fixed 2026-11-14. Check each without a test-managed transaction.

## 5. Stale docs and dead code

- [ ] `DEPLOY.md` still describes Docker/GHCR deploys and a
      `claude_routine` branch; `Dockerfile` and `docker-compose.yml` are
      still in the repo although `CLAUDE.md` says they were removed.
- [ ] `CLAUDE.md` and `README` point at `db/init.sql`, which is not in the
      repo.
- [ ] `master/PLAN.md`, `BACKLOG.md`, `MASTER_ACTIONS.md` describe the old
      "MailIM" product and four-tier pricing.
- [ ] Dead code: `ConversationService.buildSenderConversation`,
      `SenderConversation`, several unused repository queries, the reply
      form in `conversation.html`, `/searches` endpoints with no caller.
      Comments reference a removed `ThreadController`.
- [ ] `robots.txt` disallows `/threads` but not `/chats`, `/account`,
      `/admin`, `/team`. PWA icons draw "MI".
