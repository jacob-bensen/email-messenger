# BACKEND

This document describes the architecture of the **email-messenger** Spring
Boot application (product name: **ConexusMail**). It is the server-side
half of a two-repo product; the mobile client lives at
`jacob-bensen/conexus-mail-mobile` and consumes (or will consume) the JSON
API described in [`API.md`](API.md).

The server owns:

- All persistence (PostgreSQL 16, Flyway-versioned schema).
- All authoritative business logic (billing, mailbox sync, thread
  building, plan-limit enforcement, team membership, quotas).
- The public marketing/website UX and the logged-in web app (Thymeleaf).
- Outbound email (SMTP), inbound email pull (IMAP + Gmail OAuth), and
  live delivery (IMAP IDLE + SSE).

## Tech stack

- **Java 21** (LTS).
- **Spring Boot 3.x** — `spring-boot-starter-web`, `-thymeleaf`,
  `-data-jpa`, `-validation`, `-mail`, `-oauth2-client`, `-security`.
- **PostgreSQL 16** (prod + local), **H2** (tests). Base
  `application.yml` reads `JDBC_DATABASE_URL` /
  `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD`;
  `application-local.yml` points at `localhost:5432/email_messenger`.
- **Flyway** for schema migrations (see
  `src/main/resources/db/migration/`).
- **Thymeleaf** for server-rendered pages; **vanilla CSS/JS** for the
  chat UX and PWA shell.
- **Jakarta Mail** for IMAP / SMTP; **Gmail API OAuth** for Google
  accounts.
- **Stripe** for subscriptions (Checkout + Billing Portal + webhook).
- **JUnit 5** + **Spring Boot Test** + **Testcontainers** (Postgres) +
  **GreenMail** for integration tests.

The scheduled routine's default "target stack" mentioned jOOQ; this
repo uses **Spring Data JPA** (with a few `NativeQuery`s) instead.
Docs describe what the code actually does, not the generic assumption.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java     # Spring Boot entry point
├── auth/           # SecurityConfig, form-login, Google OAuth start/callback,
│                   # password reset, email verification, account settings
├── web/            # Thymeleaf controllers: /chats, /mailboxes, /pricing,
│                   # SEO, PWA shell, legal, demo, saved searches, SSE stream
├── service/        # Domain services: ConversationService, IMTransformService,
│                   # EmailBodyCleaner, ReplyService, view-model records
│                   # (ChatConversation, BubbleRun, ConversationListItem, …)
├── domain/         # JPA entities (User, MailAccount, EmailThread, Message,
│                   # Participant, Subscription, Team, TeamInvite, …) +
│                   # enums (Plan, AuthType, RecipientType, CancellationReason)
├── repository/     # Spring Data repositories
├── email/          # IMAP polling / IDLE, SMTP send, MIME parsing, Gmail API
├── billing/        # Stripe checkout, portal, webhook, subscription lifecycle
├── team/           # Team invites, membership
├── digest/         # Weekly digest + opt-out
├── admin/          # /admin/revenue dashboard, win-back outreach
├── activation/     # New-user activation drip
└── reengagement/   # Trial-end conversion, canceled-user win-back

src/main/resources/
├── templates/      # Thymeleaf views (see WEB.md for full inventory)
├── static/         # CSS, JS, PWA icons
├── db/migration/   # V1__init.sql … V35__mail_account_signature.sql
├── legal/          # Privacy / Terms / Refund boilerplate (env-overridable)
└── application.yml # Base config; dev/local/prod profiles alongside
```

## Domain model

Central entities (each mapped to a JPA class in `com.emailmessenger.domain`):

- **User** — the account owner; identified by email, may be
  password-set or Google-linked (`google_subject`); optionally an admin.
- **MailAccount** — a mailbox the user has connected. `auth_type` is
  `IMAP_PASSWORD` or `GOOGLE_OAUTH`; the IMAP password is encrypted at
  rest with `MAILBOX_ENCRYPTION_PASSWORD`/`SALT`. Tracks
  `last_seen_uid` (INBOX cursor) and `last_seen_sent_uid` (Sent folder
  cursor).
- **EmailThread** — a group of messages sharing a threading chain
  (`Message-ID` / `In-Reply-To` / `References`). Carries a
  `conversation_key` derived from the deduplicated participant set —
  this is what powers the texting-style `/chats/{key}` grouping (see
  `ConversationKeyService`).
- **Message** — one email. Body is kept in `body_plain` + `body_html`;
  `IMTransformService` + `EmailBodyCleaner` strip quoted replies and
  signatures to produce the bubble text at render time.
- **MessageRecipient** — recipients per message (TO/CC/BCC).
- **Participant** — a deduplicated email address + display name.
- **Attachment** — file metadata per message. `blob_ref` is presently
  always null; attachment bytes are not yet stored (roadmap item).
- **Subscription** — the Stripe subscription mirror: `plan` (Free / Pro
  a.k.a. Personal / Business a.k.a. Team / Enterprise), status, billing
  period, cancellation reason, win-back stamp.
- **Team**, **TeamMember**, **TeamInvite** — shared workspaces on the
  Team/Business plan.
- **SavedSearch** — user-defined saved query in `/chats`.
- **AuthEvent** — audit trail for login-related events.
- **PasswordResetToken**, **EmailVerificationToken** — one-shot tokens.
- **PlanChangeEvent** — audit trail for plan transitions.

View-model records (in `service/`, all Java `record`s) are never
persisted; they compose entity data for Thymeleaf:

- `ChatConversation`, `ChatMember`, `Conversation`, `SenderConversation`,
  `SenderView`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`,
  `SenderBubbleRun`, `ThreadRef`.

## Auth model

`com.emailmessenger.auth.SecurityConfig`:

- **Session cookie** (Spring Security default) is the credential.
- **Form login** at `POST /login` — email + password.
- **Google OAuth 2.0 login** at `GET /auth/google/start` → OAuth 2 code
  flow → session created. Wired only when
  `AUTH_GOOGLE_CLIENT_ID`/`AUTH_GOOGLE_CLIENT_SECRET` (or the legacy
  `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`) are set — otherwise the
  "Continue with Google" button is hidden.
- **CSRF** is enabled for POST forms (Thymeleaf pages embed the token).
- Public routes: `/`, `/pricing`, `/demo`, `/login`, `/register`,
  `/password/**`, `/verify-email`, `/privacy`, `/terms`, `/refund`,
  `/robots.txt`, `/sitemap.xml`, PWA assets, `/billing/webhook`
  (verified via Stripe signature).
- Everything else requires an authenticated session.

There is **no token-based (JWT / OAuth 2 bearer / API key) auth today**.
Adding a mobile client requires a token-auth path — see
[`API.md`](API.md#authentication-tbd) and
[`../INTERNAL_TODO.md`](../INTERNAL_TODO.md).

## Mailbox sync

`com.emailmessenger.email` covers pulling mail in and sending it out:

- **`MailboxPollingService`** — `@Scheduled` cron that pulls new
  messages for every connected mailbox, both INBOX and Sent folder
  (via UID cursors on `MailAccount`). Publishes a `NewMailEvent` on
  each newly persisted message.
- **`ImapIdleService`** — long-lived IMAP IDLE connection per paid-tier
  mailbox (feature-flagged via `mailbox.idle.enabled`) for
  near-instant delivery. Falls back to polling when IDLE is
  unsupported.
- **`ChatEventStream`** — Server-Sent Events at `GET /chats/events`;
  subscribed by open `/chats` tabs. Consumes `NewMailEvent` and pushes
  incremental refreshes.
- **`ReplyService`** — SMTP send for inline replies from
  `POST /chats/{key}/send`.
- **Gmail OAuth** replaces IMAP password auth for Google accounts and
  uses the Gmail API for pull (and SMTP w/ XOAUTH2 for send).

## Billing

`com.emailmessenger.billing`:

- **`BillingController`** — `POST /billing/checkout` (Stripe Checkout
  session), `POST /billing/portal` (Customer Billing Portal), plus the
  `/billing/success`, `/billing/cancel`, `/billing/cancel-subscription`
  UI.
- **`BillingWebhookController`** — `POST /billing/webhook`; the only
  endpoint that verifies a Stripe signature and mutates
  `Subscription` outside of a logged-in session.
- Plans are enumerated in `Plan.java`. Plan limits (mailbox count,
  saved-search count, etc.) are enforced server-side via
  `PlanLimitKind`.

## How Thymeleaf pages relate to services

Rule: **the same service layer answers both web and API paths**. When
the JSON API is built, controllers must delegate to the same
`ConversationService` / `ReplyService` / `MailboxService` /
`SubscriptionService` methods the Thymeleaf controllers already call.
Never fork business logic into a new "api" service just to change the
DTO shape — write a mapper from the existing view models
(`ChatConversation` etc.) to JSON DTOs instead.

`ConversationController` (`/chats/**`) is the canonical example: it
delegates all state changes to `ConversationService` and
`ReplyService`; its controller code is model-building and template
selection only.

## Building, running, testing

See `README.md` for the full commands; the essentials:

- **Build:** `./mvnw clean package`
- **Local run:**
  `JDBC_DATABASE_PASSWORD=… ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- **Tests:** `./mvnw test`
- Testcontainers-driven integration tests skip when Docker is not
  present, so `./mvnw test` is a valid smoke test on a bare box.

## Related repositories

- **`jacob-bensen/conexus-mail-mobile`** — Flutter mobile client for
  ConexusMail (currently a bare README; see
  [`../conexus-mail-mobile/docs/DESIGN.md`](../../conexus-mail-mobile/docs/DESIGN.md)
  in a side-by-side checkout).
- **`docs/API.md`** — the JSON API contract that binds the two repos.
- **`docs/WEB.md`** — the Thymeleaf website page inventory.
- **`master/PLAN.md`** — the current Primary Objective and milestones
  for the pre-launch conversion / churn work (independent of the
  two-repo sync routine).
- **`master/MASTER_ACTIONS.md`** — items blocked on a human (env
  vars, third-party credentials, legal review, launch assets).
