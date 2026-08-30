# BACKEND — architecture

Reverse-engineered from the code on 2026-08-30. This is the operator-and-developer
view; product design lives in the mobile repo's `docs/DESIGN.md`, and the shared
wire contract lives in [`docs/API.md`](./API.md).

## What this service is

`email-messenger` (product name **ConexusMail**) is a Spring Boot 3 monolith that:

1. Imports emails from user-connected IMAP mailboxes (password IMAP or Gmail
   OAuth), threads them by RFC 822 headers, and stores them in Postgres.
2. Renders threads as an IM-style conversation UI over server-rendered
   Thymeleaf pages under `/chats/**`.
3. Handles auth (form login, Google OIDC, email verification, password reset),
   Stripe-backed subscriptions (Free / Pro / Business), team invites, saved
   searches, digests, and admin metrics.
4. Ships a PWA shell for the web client. There is currently **no dedicated
   REST/JSON API** for a mobile client — the mobile app in the sibling repo
   is a fresh scaffold that will drive the API contract as it grows.

## Runtime shape

- Java 21 LTS on Spring Boot 3.5.
- PostgreSQL 16 in prod and local dev; H2 in unit/slice tests.
- Flyway manages the schema — migrations are immutable once shipped;
  add a new `V{n}__*.sql` file, never edit an existing one.
- Persistence is JPA (Spring Data). jOOQ is **not** currently used; the sync
  routine's generic guidance about jOOQ codegen does not apply here.
- Scheduled jobs (`@Scheduled`) handle mailbox polling and reengagement.
  IMAP IDLE is used for paid-tier accounts when `mailbox.idle.enabled`.
- Server-Sent Events at `/chats/events` push in-place updates to open
  browser tabs.

## Package layout (as of 2026-08-30)

```
com.emailmessenger
├── EmailMessengerApplication.java
├── activation/      # onboarding + activation-nudge scheduling
├── admin/           # /admin/**: revenue metrics, reconciliation, win-back
├── auth/            # login, register, OAuth start, password reset,
│                    #   email verification, account settings, OAuth callback
├── billing/         # Stripe checkout, portal, webhook, plan enforcement
├── digest/          # weekly-digest opt-out link handler
├── domain/          # JPA entities (User, MailAccount, EmailThread,
│                    #   Message, MessageRecipient, Participant, Attachment,
│                    #   Team, TeamMember, TeamInvite, SavedSearch,
│                    #   Subscription, Plan, PlanChangeEvent,
│                    #   AuthEvent, EmailVerificationToken,
│                    #   PasswordResetToken, DigestEmailPreference,
│                    #   CancellationReason, AuthType, RecipientType)
├── email/           # IMAP / Gmail-OAuth mailbox integration + polling
├── reengagement/    # dormancy / win-back email logic
├── repository/      # Spring Data JPA repositories
├── service/         # email parsing, IM transform, threading,
│                    #   ChatConversation, ConversationService, ReplyService,
│                    #   EmailBodyCleaner, ConversationKeyService
├── team/            # team invite web flow
└── web/             # Thymeleaf controllers (@Controller), view models,
                     #   PWA manifest, SEO, chat SSE, form objects
```

`src/main/resources/` has `templates/` (Thymeleaf), `static/` (CSS/JS),
`db/migration/` (Flyway), plus `application.yml` / `application-local.yml`
and `legal/` copy.

## Domain model (as implemented)

Core:

- **User** — one per human. Carries auth-related booleans (email verified,
  password set), acquisition source, activation-nudge flags.
- **MailAccount** — one row per connected mailbox owned by a `User`.
  Carries `AuthType` (IMAP password / Gmail OAuth), encrypted credentials,
  polling policy, `last_seen_uid` / `last_seen_sent_uid` (V6, V31 cursors),
  and an optional user-set signature (V35).
- **EmailThread** — grouping of messages linked by `Message-ID` /
  `In-Reply-To` / `References`. Carries an owner user, unread flag (V9),
  and a `conversationKey` (V32) — the sorted normalized-address set of
  participants; this is what pins a chat in `/chats/**`.
- **Message** — one imported email. Sender, subject, sent-at, plain +
  html bodies, and an outbound flag (V30 for user-sent messages).
- **MessageRecipient** — join to `Participant` with a `RecipientType`
  (TO / CC / BCC).
- **Participant** — deduped normalized email address + display name.
- **Attachment** — filename, mime, size, `blob_ref` (currently always
  null — see the roadmap: attachments are not downloadable yet).

Auth / billing / social:

- **AuthEvent** / **AuthEventType** (V16) — audit log for sign-in events.
- **EmailVerificationToken** (V15), **PasswordResetToken** (V14).
- **Subscription** + **Plan** — the local mirror of Stripe subscriptions;
  **PlanChangeEvent** (V26) records plan transitions, **CancellationReason**
  (V27) the "why did you cancel" answers.
- **Team** / **TeamMember** / **TeamInvite** / **TeamMemberRole** (V24).
- **SavedSearch** (V10, V11) — user-defined chat filters.
- **DigestEmailPreference** (V12) — weekly digest opt-in/out.

Presentation / view models (records under `service/`):

- **Conversation** — messages sorted, quoted replies stripped, consecutive
  same-sender messages grouped into `BubbleRun`s.
- **ChatConversation** — the `/chats` model, keyed by `conversationKey`.
- **ConversationListItem** — the rail item.
- **SenderConversation** / **SenderView** — the per-sender variant.

## Auth model

- Web: Spring Security form login with `RememberMe`, Google OIDC via
  `spring-boot-starter-oauth2-client`. Password hashes are BCrypt.
- Session cookies today; there is no token or JWT issuer yet. When the
  mobile REST API lands (see the roadmap), it will need a token/session
  strategy — that decision is deferred to the first mobile-API PR and
  will be recorded in `docs/API.md`.
- Authorization: `/admin/**` is gated by the allowlist in
  `application.yml`'s `admin.emails`. Ownership on chats/mailboxes is
  enforced in the service layer by loading rows scoped to the current
  user.

## Persistence and money-safe operations

- State mutations run inside `@Transactional` service methods; controllers
  do not open transactions or make direct repository writes for anything
  economy-relevant.
- Stripe events are handled in `BillingWebhookController` and reconciled
  through `BillingService`; the webhook signature is validated using
  `STRIPE_WEBHOOK_SECRET`.
- Mailbox credentials are encrypted at rest using `MAILBOX_ENCRYPTION_PASSWORD`
  / `MAILBOX_ENCRYPTION_SALT` (dev fallback key if unset — see README).

## Website ↔ services

Thymeleaf controllers under `web/` (and `auth/`, `billing/`, `team/`,
`digest/`, `admin/`) render pages and consume the same services the REST
API will use. Business logic lives in `service/` and in the domain-owning
subpackages (`billing/BillingService`, `team/TeamInviteService`,
`auth/*Service`, `email/*`). When a mobile REST endpoint is added, it MUST
call the same service — never fork logic into the controller.

See [`docs/WEB.md`](./WEB.md) for the page inventory.

## Related repositories

- **jacob-bensen/conexus-mail-mobile** — Flutter mobile client. Its
  `lib/domain/` mirrors the server's rules; the wire contract is
  [`docs/API.md`](./API.md), shared with that repo.
