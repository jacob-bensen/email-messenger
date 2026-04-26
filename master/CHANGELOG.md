# Changelog

## 2026-04-26 — Autonomous Run #4

### Role 1 — Feature Implementer
**Task completed**: IM transform — quoted-reply stripping, same-sender bubble grouping, markdown rendering

Files created:
- `src/main/java/com/emailmessenger/service/IMTransformService.java` — package-private `@Service`:
  - `stripQuotes(String)`: line-by-line parser; detects "On ... wrote:" attribution (including
    wrapped multi-line variants), Outlook "-----Original Message-----" divider, and `> ` prefixed
    quote lines; cuts everything from the first attribution/divider onwards; collapses 3+ blank
    lines to 2; strips surrounding whitespace.
  - `renderMarkdown(String)`: HTML-escapes input first (prevents XSS in plain-text path), then
    applies regex transforms for **bold**, *italic*, `code`, and URL auto-linking; wraps in `<p>`
    tags at blank-line boundaries, single newlines become `<br>`.
- `src/main/java/com/emailmessenger/service/ConversationService.java` — public `@Service`:
  - `buildConversation(EmailThread)` — iterates thread's messages (already `@OrderBy sentAt ASC`),
    groups consecutive same-sender messages into `BubbleRun` instances (compared by email address,
    not DB ID, so works on unsaved entities in tests), returns an immutable `Conversation`.
  - `buildBodyHtml(Message)` — prefers `bodyHtml` from the email if present; otherwise strips
    quotes then renders plain text as markdown HTML.
- `src/main/java/com/emailmessenger/service/BubbleMessage.java` — package-private record:
  `(messageId, bodyHtml, sentAt, attachments)`.
- `src/main/java/com/emailmessenger/service/BubbleRun.java` — package-private record:
  `(sender, messages)`.
- `src/main/java/com/emailmessenger/service/Conversation.java` — public record:
  `(thread, runs)`.

Verified: `./mvnw test` → BUILD SUCCESS, 40 tests pass.

**Income relevance**: The IM transform is the product's primary value proposition — without it,
threads display as raw email walls of text, not chat bubbles. This layer directly gates all
UI work that drives conversion.

---

### Role 2 — Test Examiner
**Coverage added**: 23 new tests (14 IMTransformService + 8 ConversationService + 1 BCC import)

Files added:
- `src/test/java/com/emailmessenger/service/IMTransformServiceTest.java` (14 tests):
  - null body, non-quoted content preserved, `>` line removal, Gmail "On ... wrote:" removal,
    wrapped 2-line attribution, Outlook divider, blank-line collapsing, bold, italic, inline
    code, URL auto-link, paragraph wrapping, HTML char escaping.
- `src/test/java/com/emailmessenger/service/ConversationServiceTest.java` (8 tests):
  - empty thread, single message, same-sender grouping, different-sender splitting,
    mixed group counts, plain-to-HTML body transform, HTML body passthrough, quoted-reply stripping.
- Added `bccRecipientsAreCaptured` test to `EmailImportServiceTest` — BCC path was exercised
  in code but had zero test coverage.

Total test count: 40 (up from 17). No failures. No flaky tests.

Income-critical paths still at zero coverage (code not written yet):
- Stripe webhook handler and subscription state
- User authentication flows
- IMAP polling job
- Thymeleaf template rendering

---

### Role 3 — Growth Strategist
Added 4 new growth tasks to INTERNAL_TODO.md not previously captured:
1. **Demo mode** [GROWTH][S] — /demo route with pre-seeded sample threads; visitors experience
   the IM view without signing up; removes top-of-funnel uncertainty. HIGH income impact.
2. **Email forwarding address** [GROWTH][M] — unique @mailaim.app address per user; forwarded
   emails auto-import; avoids IMAP credential friction entirely. HIGH income impact.
3. **EML file upload** [GROWTH][S] — upload a .eml file to seed threads instantly; zero-friction
   demo path. MEDIUM income impact.
4. **Thread labels/tags** [GROWTH][M] — user-defined labels; Team plan feature gate. MEDIUM income
   impact; drives Personal → Team upgrade.

Added 2 [MARKETING] items to TODO_MASTER.md:
- Loom/YouTube demo video: highest-leverage single landing-page conversion asset.
- Chrome/Firefox extension: "Open in MailIM" button in Gmail = viral distribution loop.

---

### Role 4 — UX Auditor
Templates still do not exist; no live user flow to walk. Two new UX issues flagged from
the new service layer:
1. **Participant initials utility** [UX][S] — no method exists to compute avatar initials from
   displayName; template work will be blocked without this helper.
2. **th:utext rendering note** [UX][S] — BubbleMessage.bodyHtml contains pre-rendered HTML;
   Thymeleaf must use th:utext not th:text; flagged for the template author to avoid a subtle
   "shows raw HTML tags" bug.

---

### Role 5 — Task Optimizer
Rewrote INTERNAL_TODO.md:
- Archived 1 new completed task (IM transform) → Done section now has 7 items
- Promoted [HEALTH][M] HTML sanitization to a new "Health / Security (pre-launch blockers)"
  section at the top — it must be resolved before templates ship
- Added 4 new [GROWTH] and 2 new [UX] tasks from this run
- Consolidated growth tasks in priority order: auth/billing/limits first, then
  onboarding/SEO/virality, then engagement, then infrastructure
- Verified no duplicates across 37 active tasks
- All tasks tagged [S/M/L]

---

### Role 6 — Health Monitor
Security (one critical finding):
- **XSS in HTML email bodies** [CRITICAL]: `ConversationService.buildBodyHtml` returns
  `msg.getBodyHtml()` directly without sanitization. When Thymeleaf renders this with `th:utext`,
  any `<script>`, event handler, or iframe in an incoming email executes in the user's browser.
  Fix: add jsoup to pom.xml and call `Jsoup.clean(bodyHtml, Safelist.relaxed())` before returning.
  Added [CRITICAL] note to TODO_MASTER.md and [HEALTH][M] task to INTERNAL_TODO.md.
- The plain-text path (`renderMarkdown`) is safe: HTML is escaped before any regex transformation,
  so no injection is possible through the markdown renderer.

Performance:
- `IMTransformService.stripQuotes` is O(n) in number of lines; no regex backtracking risk since
  all patterns are simple anchored or character-class patterns.
- `ConversationService.buildConversation` is O(n) in messages; produces `List.copyOf` snapshots —
  immutable and safe to cache.

Code quality:
- `IMTransformService` and view model records (`BubbleMessage`, `BubbleRun`) are package-private —
  correctly scoped; only `ConversationService` and `Conversation` are public.
- Constructor injection in `ConversationService`; no field `@Autowired`.
- No dead code. All 5 new files are exercised by tests.

Dependencies:
- No new runtime dependencies added this run. jsoup needed for the HTML sanitization fix (tracked).

Legal: no change from prior runs — all open [LEGAL] items in TODO_MASTER.md remain outstanding.

---

## 2026-04-26 — Autonomous Run #3

### Role 1 — Feature Implementer
**Task completed**: Email-import service — RFC 822 parsing + thread building

Files created:
- `src/main/java/com/emailmessenger/email/ParsedEmail.java` — package-private record holding
  all parsed fields from a MimeMessage before DB operations: messageId, inReplyTo, references
  (List<String>), subject, from, to/cc/bcc recipients, bodyPlain, bodyHtml, attachments, sentAt.
  Two nested records: `AddressEntry(email, name)` and `AttachmentEntry(filename, mimeType, sizeBytes)`.
- `src/main/java/com/emailmessenger/email/MimeMessageParser.java` — package-private class that
  walks a `MimeMessage` tree: extracts RFC 5322 headers, recursively walks `multipart/*` to find
  `text/plain` / `text/html` parts, collects attachment metadata (filename, mimeType, size).
  Does NOT read attachment byte content — only metadata; blob_ref is wired separately.
- `src/main/java/com/emailmessenger/email/EmailImportService.java` — `@Service` with
  `@Transactional` import method:
  - Idempotent: skips if Message-ID already exists in the database.
  - Thread resolution: walks References list newest-first per RFC 5322, falls back to In-Reply-To,
    then rootMessageId lookup, then creates a new EmailThread.
  - `resolveParticipant()`: find-or-create Participant by normalised email address.
  - Attachment metadata captured; `blob_ref` is null until external storage is wired.
  - Save order: `messageRepo.save(message)` first so the subsequent `threadRepo.save(thread)`
    cascade-merges an already-managed entity (avoids double-INSERT unique constraint violation).

Bug found and fixed during test run: original code called `thread.addMessage(message)` +
`threadRepo.save(thread)` (which cascades ALL to the unsaved message entity, inserting it), then
called `messageRepo.save(message)` again — violating the `message_id_header` unique constraint.
Fix: save message first, then add to thread, then save thread.

Verified: `./mvnw test` → BUILD SUCCESS, 17 tests pass.

**Income relevance**: Email parsing and thread building are the core data pipeline. Without this,
no thread can ever appear in the UI — it is the prerequisite for all user-visible value.

---

### Role 2 — Test Examiner
**Coverage added**: 6 new tests in `EmailImportServiceTest` (17 total, up from 11)

File created: `src/test/java/com/emailmessenger/email/EmailImportServiceTest.java`
- `importCreatesThreadAndMessage` — verifies thread, message, sender, body, rootMessageId
- `replyViaInReplyToJoinsExistingThread` — verifies In-Reply-To links reply to parent thread; checks messageCount=2
- `replyViaReferencesJoinsExistingThread` — verifies References header thread linking
- `duplicateMessageIdIsSkipped` — verifies idempotency returns empty + only one DB row
- `senderParticipantIsDeduplicated` — verifies two messages from same address → 1 Participant
- `toAndCcRecipientsAreCaptured` — verifies RecipientType.TO (×2) and CC (×1) stored correctly

Tests use `@SpringBootTest @ActiveProfiles("dev") @Transactional` with H2; each test rolls back.
MimeMessage instances built in-process via `jakarta.mail.Session.getInstance(new Properties())`.

Income-critical paths with zero coverage (code not yet written — expected):
- Stripe webhook handler and subscription state
- User authentication flows
- IM transform (quoted-reply stripping)
- IMAP polling job

No flaky tests. No redundant tests. No test failures.

---

### Role 3 — Growth Strategist
Added 5 new growth tasks to INTERNAL_TODO.md (not previously captured):
1. **Thread permalink sharing** [GROWTH][M] — shareable read-only `/share/{token}` link is a
   viral touchpoint; every person who receives a link gets a demo of the product. HIGH income impact.
2. **Browser push notifications** [GROWTH][S] — Web Push API re-engages users without requiring
   them to keep the tab open; drives daily active usage. MEDIUM income impact.
3. **Slack/Discord webhook integration** [GROWTH][M] — sends a Slack/Discord notification when
   a new email arrives; this is a natural $29/mo Team plan feature gate. HIGH income impact.
4. **Thread export (PDF/HTML)** [GROWTH][M] — freelancers and support teams need to export
   conversations; this is a paid-tier feature gate. MEDIUM income impact.
5. **In-app referral prompt** [GROWTH][S] — triggered after activation milestone (10 threads
   imported); shows tweet + copy-link modal. MEDIUM income impact.

Added 2 [MARKETING] items to TODO_MASTER.md:
- Slack/Discord community for early users
- Channel-specific announcement when webhook integration ships

---

### Role 4 — UX Auditor
No Thymeleaf templates exist yet so no live user flows to walk. All 5 previously flagged UX
issues remain in INTERNAL_TODO.md. Added 2 new UX issues specific to the import service now built:
1. **Import error feedback** [UX][S] — MessagingException/IOException from import currently
   bubbles up as a 500. Needs a user-visible error banner or toast, not a crash page.
2. **IMAP sync status indicator** [UX][S] — thread list should show "last synced X minutes ago"
   so users know whether they're seeing live data; critical for trust in the product.

---

### Role 5 — Task Optimizer
Audited and rewrote INTERNAL_TODO.md:
- Archived 1 new completed task (Email Import Service) → Done section now has 6 items
- Added 5 new [GROWTH] tasks from Role 3 in priority order (below existing Growth items)
- Added 2 new [UX] tasks from Role 4
- Added 1 new [HEALTH] task: wrap mail exceptions in domain exception
- Confirmed no duplicates across 30 active tasks
- All tasks remain tagged [S/M/L]; no vague or oversized tasks
- No blocked tasks

---

### Role 6 — Health Monitor
Security (no critical issues):
- No hardcoded credentials in any new file
- `MimeMessageParser` handles null Message-ID, null sender, null sent date defensively
- `resolveParticipant` normalises email to lowercase before lookup/storage — prevents duplicate
  Participants from case-variant addresses (e.g. Alice@Test.com vs alice@test.com)
- `importMessage` is idempotent on Message-ID — safe to call from a polling job without
  duplicate-import protection at the job level

Performance:
- `walkParts` recursion depth is bounded by the message structure (typically 2–3 levels);
  no risk of stack overflow for standard RFC 822 messages
- `resolveParticipant` does a SELECT before every INSERT — acceptable for import volumes,
  but may become a bottleneck in bulk-import scenarios (flagged for future batching)
- `resolveThread` does up to N+1 SELECTs for messages in the References list (worst case:
  long References chain with no match). Low risk at current scale; indexed on message_id_header.

Code quality:
- `MimeMessageParser` is package-private (correctly scoped)
- `ParsedEmail` is a record — immutable, no mutable state
- Constructor injection enforced; no `@Autowired` fields
- `(String) part.getContent()` cast in `walkParts` is guarded by `isMimeType("text/plain")`
  and `isMimeType("text/html")` checks — Jakarta Mail guarantees String content for those types

Flagged:
- `MessagingException` and `IOException` leak through `EmailImportService.importMessage()` as
  checked exceptions — added [HEALTH][S] task to wrap in a domain exception
- No rate limiting on import volume per user — will need enforcement when IMAP polling ships

Legal (no change from Run #2 — all items still open in TODO_MASTER.md [LEGAL]).

---

## 2026-04-26 — Autonomous Run #2

### Role 1 — Feature Implementer
**Tasks completed**: Flyway V1__init.sql + domain entities + Spring Data repositories

Files created:
- `src/main/resources/db/migration/V1__init.sql` — 5 tables (participants, email_threads,
  messages, message_recipients, attachments) with FK constraints and 6 performance indexes.
  SQL is ANSI-standard `GENERATED BY DEFAULT AS IDENTITY` — runs on both H2 (test) and
  PostgreSQL (production) without a compatibility layer.
- `src/main/java/com/emailmessenger/domain/` — 6 files:
  - `Participant.java` — `@Entity`, deduped by email, `@PrePersist` createdAt
  - `EmailThread.java` — `@Entity`, `@OneToMany messages`, `@PrePersist/@PreUpdate` timestamps,
    `addMessage()` helper increments messageCount
  - `Message.java` — `@Entity`, `@ManyToOne thread + sender`, `@OneToMany recipients + attachments`,
    `addRecipient()` / `addAttachment()` helpers
  - `MessageRecipient.java` — `@Entity`, `@ManyToOne message + participant`, `RecipientType` enum
  - `Attachment.java` — `@Entity`, `@ManyToOne message`, blob_ref for storage pointer
  - `RecipientType.java` — enum `TO / CC / BCC`
- `src/main/java/com/emailmessenger/repository/` — 4 interfaces:
  - `ParticipantRepository` — `findByEmail`, `existsByEmail`
  - `EmailThreadRepository` — `findByRootMessageId`, `findAllByOrderByUpdatedAtDesc(Pageable)`
  - `MessageRepository` — `findByMessageIdHeader`, `findByThreadIdOrderBySentAtAsc`
  - `AttachmentRepository` — `findByMessageId`

Verified: `./mvnw test` → BUILD SUCCESS, all tests pass.

**Income relevance**: Domain model and persistence layer are the foundation for all features
that drive revenue — user threads, message history, attachment storage. Nothing billable ships
without this layer.

---

### Role 2 — Test Examiner
**Coverage added**: 10 new tests across 3 test classes (11 total, up from 1).

Files added:
- `ParticipantRepositoryTest` (3 tests): save/find by email, existsByEmail, unique constraint enforcement
- `EmailThreadRepositoryTest` (4 tests): save/find thread, ordered by updatedAt, message persistence, findByMessageIdHeader
- `AttachmentRepositoryTest` (3 tests): save/find by message ID, multiple attachments per message, TO/CC recipient types

Income-critical paths still at zero coverage (code not yet written — expected):
- Stripe webhook handler and subscription state transitions
- User authentication (login, registration, session)
- IMAP ingestion and thread-building logic
- IM transform (quoted-reply stripping)

No flaky tests. No redundant tests. No test failures. Added TEST-FAILURE tagging protocol to
INTERNAL_TODO.md for when those paths are built.

---

### Role 3 — Growth Strategist
Added 5 new implementable growth tasks to INTERNAL_TODO.md:
1. **Google OAuth SSO** [GROWTH][S] — Removes password friction at signup; Gmail users get
   mailbox auto-connected. HIGH income impact. Added credential step to TODO_MASTER.md.
2. **First-run onboarding wizard** [GROWTH][M] — Guided "connect your mailbox" flow.
   Reduces activation drop-off = more users reach the Aha moment = more conversions.
3. **Upgrade prompts at limit** [GROWTH][S] — Inline modal when free tier hits 500 threads
   or 1 mailbox. Direct revenue trigger. HIGH income impact.
4. **Email digest notifications** [GROWTH][M] — Daily/weekly re-engagement email. Reduces
   churn. MEDIUM income impact.
5. **SEO landing page** [GROWTH][M] — Static / with features + pricing. Organic traffic
   from "email as chat" searches. MEDIUM income impact.

Added [MARKETING] Google OAuth ToS review item to TODO_MASTER.md.

---

### Role 4 — UX Auditor
No Thymeleaf templates exist yet so no live user flows to audit. Flagged 5 specific
UX issues for when templates are built (added to INTERNAL_TODO.md tagged [UX]):
1. Thread list empty state — blank page with no CTA when no threads exist
2. Conversation empty state — blank panel when thread has no messages
3. Error pages — Spring Whitelabel error page exposed to users; needs custom error.html
4. Reply button prominence — must be primary action, not buried below message list
5. Mobile layout — 375px viewport pass required for chat bubbles

---

### Role 5 — Task Optimizer
Audited and rewrote INTERNAL_TODO.md:
- Archived 5 completed tasks to Done section
- Organized into sections: Core, Growth/Revenue, UX, Health, Infrastructure
- Consolidated duplicate upgrade-prompt mentions into one task
- Re-prioritized: Core income-blocking features first, then Growth, UX, Health, Infra
- All tasks tagged [S/M/L]; no oversized tasks remain
- No blocked tasks (all dependencies either done or not yet needed)

---

### Role 6 — Health Monitor
Security audit (no issues found):
- No hardcoded credentials in any source file
- `application.yml` prod profile uses env-var placeholders throughout
- No `@Autowired` field injection (constructor injection enforced by convention)
- `open-in-view=false` prevents lazy-load-over-HTTP session leaks

Performance review:
- 6 indexes added in V1__init.sql covering all FK columns and common sort keys
  (`updated_at DESC`, `sent_at DESC`) — N+1 risk on unindexed FKs eliminated at schema level
- `FetchType.LAZY` on all `@ManyToOne` and `@OneToMany` relations — no eager cross-table fetches
- No unbounded list queries; `EmailThreadRepository.findAllByOrderByUpdatedAtDesc` uses `Pageable`

Code quality:
- No dead code; all 6 entity classes and 4 repositories are actively referenced in tests
- `Collections.unmodifiableList()` on collection getters in EmailThread and Message —
  prevents external mutation of managed collections
- No unused dependencies

Dependencies:
- Spring Boot 3.5.14 (latest 3.x); no known CVEs in current dep set
- H2 2.3 (bundled by Spring Boot) — no known issues
- Testcontainers 1.20.4 — current stable

Legal (no change from Run #1):
- Privacy Policy, Terms of Service, Refund Policy, Cookie banner still outstanding
- Flagged in TODO_MASTER.md [LEGAL] — no action taken (code-level task; requires Master)

## 2026-04-25 — Autonomous Run #1 (Bootstrap)

### First Run — APP_SPEC.md created
- Defined application: **MailIM** — email-to-instant-message SaaS
- Business model: Freemium subscription ($0 / $9 / $29 / $99 per month)
- Target users: remote workers, support teams, freelancers

### Role 1 — Feature Implementer
**Task implemented**: Scaffold Maven project + Add starters (CLAUDE.md items 2 and 3)

Files created:
- `pom.xml` — Spring Boot 3.5.14, Java 21; starters: web, thymeleaf, data-jpa,
  validation, mail, flyway-core, flyway-database-postgresql; runtime: postgresql;
  test: h2, spring-boot-starter-test, testcontainers-bom, testcontainers-postgresql
- `.mvn/wrapper/maven-wrapper.properties` — Maven 3.9.11
- `mvnw` / `mvnw.cmd` — generated by `mvn wrapper:wrapper`
- `src/main/java/com/emailmessenger/EmailMessengerApplication.java`
- `src/main/resources/application.yml` — dev profile (H2 in-memory) and prod profile
  (PostgreSQL via env vars DB_URL / DB_USER / DB_PASS)
- `src/test/java/com/emailmessenger/EmailMessengerApplicationTests.java`

Verified: `./mvnw test` → BUILD SUCCESS, 1 test, 0 failures.

**Income relevance**: Compiling foundation for all income-generating features.

### Role 2 — Test Examiner
- Reviewed test suite: 1 test (`contextLoads`) passes; application context boots
  correctly against H2 with Flyway (0 migrations, schema empty — expected).
- Income-critical paths with zero test coverage (expected — code not yet written):
  - Stripe payments / billing webhooks
  - User authentication and registration
  - IMAP email ingestion and thread building
  - IM transform (quoted-reply stripping)
- No flaky or redundant tests. No test failures.
- Added test coverage tasks to INTERNAL_TODO.md for when features are built.

### Role 3 — Growth Strategist
Identified highest-leverage income opportunities and added to INTERNAL_TODO.md:
1. **Stripe billing** [GROWTH][M] — direct revenue unlock; no paying users without it
2. **User auth** [GROWTH][M] — prerequisite for billing; needed for multi-tenancy
3. **Plan-limit enforcement** [GROWTH][S] — creates upgrade pressure on free users
4. **Gravatar + initials avatars** [GROWTH][M] — improves perceived quality → conversion
5. **Unread tracking** [GROWTH][S] — engagement + daily active usage driver
6. **Full-text search** [GROWTH][M] — key feature gate for Personal/Team upgrade
7. **Referral link** [GROWTH][S] — viral loop for organic growth

Added marketing actions to TODO_MASTER.md: Product Hunt, directory listings, SEO
content, community posts, waitlist landing page.

### Role 4 — Task Optimizer
- Created INTERNAL_TODO.md with full task backlog in priority order
- Archived completed scaffold tasks
- All tasks tagged by priority, size, and category
- No duplicates found (first run)
- Blocked tasks: none yet

### Role 5 — Health Monitor
Security:
- No hardcoded credentials found in any file
- `application.yml` prod profile uses `${DB_URL}`, `${DB_USER}`, `${DB_PASS}`,
  `${MAIL_HOST}`, `${MAIL_USER}`, `${MAIL_PASS}` env-var placeholders — safe
- No payment flows present yet — no PCI exposure

Performance:
- No entities or queries yet — N+1 analysis deferred to when entities exist
- `spring.jpa.open-in-view=false` set in both profiles (avoids lazy-load antipattern)

Dependencies:
- Spring Boot 3.5.14 (latest 3.x stable as of 2026-04-25)
- No known CVEs flagged in current dep set (Hibernate 6.6.49, Flyway bundled)

Legal (flagged in TODO_MASTER.md [LEGAL]):
- No Privacy Policy page
- No Terms of Service page
- No Refund Policy page
- Cookie consent banner not yet implemented
- All above required before accepting payments or EU/CCPA-covered users

Code quality:
- Only 3 Java files; no dead code, no repeated logic
- `open-in-view=false` already set to prevent common JPA antipattern
