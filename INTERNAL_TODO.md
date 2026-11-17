# Internal TODO

Known gaps, tech debt and deferred work, most severe first. The sync routine
consumes from the top and appends what it discovers. Product roadmap lives in
`conexus-mail-mobile/docs/DESIGN.md` §ROADMAP (mobile) and `CLAUDE.md`
(website).

## 1. Contract breakage (client ↔ server)

- [ ] **API sends are not idempotent and carry no attachments**
      (`docs/API.md` §5.8). A client retry after a timeout can send twice;
      add an `Idempotency-Key` (or client message id) and multipart
      attachments.
- [ ] **Mail preferences and signatures are website-only.** Sync
      frequency, the default sending mailbox and per-mailbox signatures
      have no API yet (`docs/API.md` §6.1); `isDefault` is read-only.
- [ ] **Gmail OAuth mailboxes cannot be connected or reconnected from the
      API** (§5.10 `authType: google`); the app can only sync/remove them.
- [ ] **Stale branches.** Hundreds of unmerged `claude/gifted-carson-*`
      branches on origin. `master` now holds the consolidated work
      (2026-11-16); the others are superseded and can be deleted by the
      owner. Two of them (`xg109u`, `7vfcku`) carry an alternate docs
      bootstrap that was not merged.
- [ ] **View models still hold JPA entities** (`BubbleMessage.attachments`
      is `List<Attachment>`, `Conversation.thread` an `EmailThread`). The
      API maps them to records inside the read transaction, so nothing
      leaks onto the wire; the view models themselves are unchanged.
- [ ] **Timestamps are zone-less.** `LocalDateTime` values are UTC by
      convention only; the API converts them to `Z` instants at the DTO
      edge (`ApiDtos`), which is right only while stored values are UTC.
- [ ] **Live events carry only a count** (`{"imported":N}`), so any client
      must refetch the whole list.

## 2. Logic drift and correctness

- [ ] **Send then record is not atomic.** `ChatSendService` records the
      outbound copy only after SMTP accepts; a crash or DB error in between
      leaves a sent email with no bubble (until the Sent-folder import, if
      the user's mailbox stores it). An outbox table would fix both this and
      idempotency.
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

- [ ] **`MailAccountService.connect` holds a DB transaction across IMAP
      I/O** (verify + initial import of 30 messages), on the website and
      `POST /api/v1/mailboxes` alike; a slow server pins a connection for
      the whole call. Split verify/import out of the transaction.
- [ ] **Connecting the same mailbox twice creates two rows** (web and
      API); no unique (user, host, username) check.

## 3. Security and operations

- [ ] No rate limit on `POST /api/v1/mailboxes` or `…/{id}/sync`: each
      call opens an IMAP connection to a user-chosen host (also true of the
      website forms).
- [ ] `GET /api/v1/conversations` (like `/chats`) fires an async mailbox
      refresh on every request; a client polling the list triggers IMAP
      work each time (bounded by the per-mailbox "due" check).
- [ ] Expired `api_tokens` rows are never pruned (rejected, but kept).
      Add a cleanup to an existing daily job. No device list / "sign out
      other devices" UI on `/account` yet.
- [ ] Changing or resetting the password does not revoke API tokens;
      disabling the user does. Decide whether a password change should.
- [ ] Google-only accounts cannot sign in on mobile (no password). Needs a
      native Google sign-in → token exchange design.

- [ ] `CredentialEncryptor` falls back to a hardcoded dev key (WARN only)
      when `MAILBOX_ENCRYPTION_*` are unset; production should refuse to
      start instead.
- [ ] `admin.emails` is hardcoded in the committed `application.yml`.
- [ ] `ClientIp` trusts the leftmost `X-Forwarded-For` value (spoofable
      without a trusted proxy). No rate limit on register, forgot-password,
      send or `/chats/refresh`. No Content-Security-Policy.
- [ ] `ChatEventStream` is in-memory: single instance only.
- [ ] Search is `LIKE '%q%'` with no full-text index; a conversation loads
      all its messages with no pagination — on the website and in
      `GET /api/v1/conversations/{key}` alike. Long histories need a
      cursor (`before=<messageId>`) before they get slow.
- [ ] `GET /api/v1/conversations/{key}` omits member signatures (the
      website shows them in the side panel); add with the signature →
      contact card roadmap item.

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
