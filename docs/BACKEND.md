# Backend architecture

The backend is a single Spring Boot 3 application (Java 21) that serves both
the marketing/product website (Thymeleaf) and — over time — a JSON REST API
for the ConexusMail mobile client. Today the REST surface is essentially
empty; see [API.md](API.md) for what exists and what still needs to be built.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java  # Spring Boot entry point
├── activation/                     # onboarding drip: nudge, follow-up, last-chance emails
├── admin/                          # admin metrics dashboard (revenue, retention)
├── auth/                           # form login, Google OIDC, email verification, password reset,
│                                   # account settings, security config
├── billing/                        # Stripe checkout, portal, webhooks, plan enforcement
├── digest/                         # weekly/monthly digest emails
├── domain/                         # JPA entities
├── email/                          # IMAP/SMTP: mailbox connect, polling, IDLE, import parser,
│                                   # signature parsing, address normalization
├── reengagement/                   # win-back emails for lapsed users
├── repository/                     # Spring Data JPA repositories
├── service/                        # cross-cutting business logic: conversation-key hashing,
│                                   # IM transform, reply/send orchestration, body cleaner
├── team/                           # team invites, team membership
└── web/                            # Thymeleaf controllers, view models, PWA, SEO, /chats workspace
```

Constructor injection everywhere; no field `@Autowired`. Records for DTOs
and view models; classes for entities. Package-private by default.

## Persistence

- PostgreSQL 16 in prod and local dev, H2 for tests.
- Spring Data JPA on top of Hibernate.
- Flyway migrations under `src/main/resources/db/migration/V{n}__*.sql`,
  currently at V35. Migrations are immutable — add a new `V{n}` rather
  than editing an existing one.
- Local bootstrap: `db/init.sql` creates the database and app user; the
  `local` profile reads `application-local.yml` and points at
  `localhost:5432/email_messenger`.
- Base `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` env vars for
  Heroku-style deploys.

## Domain model (the shapes the mobile client will need)

- **User** — login identity: email, bcrypt password (nullable for pure OIDC
  users), display name, plan (Free/Pro/Business), email-verified flag,
  Google subject, default mail account, appearance settings, activation
  and reengagement tracking timestamps.
- **MailAccount** — one connected mailbox owned by a User. Holds IMAP/SMTP
  host + port + username + credentials (encrypted at rest), auth type
  (`PASSWORD` or `GOOGLE_OAUTH`), polling cursor (`lastSeenUid`,
  `lastSeenSentUid`), signature text.
- **EmailThread** — one RFC 822 threading unit (built from `Message-ID` /
  `In-Reply-To` / `References`). Owned by the User; carries
  `conversationKey` (participant-set hash) and unread flag.
- **Message** — one email inside a thread. Sender + recipients (via
  `MessageRecipient`), plain and HTML body, sent-at, headers.
- **Participant** — normalized email address + display name + avatar hash.
  Deduped app-wide (not per-user); a display name is per-message.
- **MessageRecipient** — join between Message and Participant, tagged with
  `RecipientType` (TO/CC/BCC/FROM/REPLY_TO).
- **Attachment** — filename, mime, size, and `blob_ref` (currently always
  null — attachments are metadata-only, not downloadable yet).
- **Subscription** — Stripe subscription: plan, status, current period
  start/end, cancel-at-period-end, cancellation reason.
- **Team / TeamMember / TeamInvite** — Business-plan multi-user grouping.
- **SavedSearch** — a saved chat-list filter (query + last-viewed cursor).
- **DigestEmailPreference** — per-user opt-in for weekly/monthly digests.
- **AuthEvent** — login attempts (success/failure/lock) for audit.
- **PlanChangeEvent** — audit of plan upgrades/downgrades for admin metrics.
- **PasswordResetToken / EmailVerificationToken** — single-use, short-TTL
  tokens.

## The `/chats` workspace (the app's main surface)

Conversations are keyed by their **participant set**, not the RFC 822
thread — see `ConversationKeyService`. A "chat with Ada and Ben" merges
every thread whose participants are `{me, ada, ben}`, so a fresh reply
from Ada that Gmail would split into a new thread still lands in the same
bubble timeline.

- `ConversationListService` renders the sidebar (the list of chats).
- `ChatService` builds one open chat: unified timeline, participant list,
  signature rail, unread state.
- `ReplyService` sends a reply on the most recent thread (keeps subject
  and threading headers).
- `OutboundMessageService` records sent messages so they show up
  immediately in the timeline, before the Sent-folder import catches up.
- `MailboxPollingService` refreshes due mailboxes; `ImapIdleService`
  runs a live IDLE loop for paid-tier accounts.
- `ChatEventStream` is a Server-Sent Events stream at `/chats/events`
  that publishes `NewMailEvent`s so open pages patch in place without
  a full reload.

## Auth model

- Spring Security with session cookies (`JSESSIONID`) — configured in
  `auth/SecurityConfig.java`.
- Form login at `POST /login` (`email` + `password`) with a
  `LoginThrottleFilter` in front of the credentials check.
- OAuth2 / OIDC login for Google, wired in only when `GOOGLE_CLIENT_ID` is
  set. `OAuth2ProvisioningService` provisions a `User` on first sign-in.
- "Remember me" cookie backed by `PersistentTokenRepository` (JDBC).
- CSRF protection is enabled everywhere except `/h2-console/**` and
  `/billing/webhook` (Stripe posts raw JSON with a signature header).
- `DbUserDetailsService` loads users by email.
- `EmailVerificationService` sends a one-time verification link;
  unverified users can still log in but see a banner on `/chats`.
- No token / JWT auth today. This is the primary blocker for the mobile
  client — see `INTERNAL_TODO.md`.

## Website ↔ API relationship

Today the website is essentially the whole app. Business logic lives in
`service/`, `email/`, `billing/`, and `auth/` — the Thymeleaf controllers
in `web/` are thin orchestrators. When the mobile API is built, it MUST
consume the same service layer, never duplicate logic. See
[WEB.md](WEB.md) for the page inventory and
[API.md](API.md) for the target contract.

## Background jobs

Spring `@Scheduled` jobs run inside the same app process:

- `MailboxPollingService.pollAll()` — polls due mailboxes on a fixed
  cadence. Behind the `mailbox.polling.enabled` flag.
- `ImapIdleService` — long-lived IDLE connections for paid-tier accounts
  (`mailbox.idle.enabled`).
- Activation / reengagement / digest email jobs — trigger their respective
  services when the schedule fires.

## Related repositories

- **jacob-bensen/conexus-mail-mobile** — Flutter mobile client (this repo's
  sibling). See its `docs/DESIGN.md` for product design and its
  `docs/MOBILE.md` for client architecture.
- Shared contract: `email-messenger/docs/API.md` is the single source of
  truth for the REST API both sides implement against.
