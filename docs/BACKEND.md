# BACKEND

The `email-messenger` repository is the ConexusMail **backend + website**.
It is a single Spring Boot 3 application that serves:

- The public marketing site and the signed-in web workspace (Thymeleaf).
- All persistence (PostgreSQL 16, Flyway migrations).
- Email import (IMAP/OAuth), sending (SMTP), threading, and IM transform.
- Billing (Stripe), auth (form + Google OIDC), teams, digests, admin.
- A REST API for the **mobile client** (`conexus-mail-mobile`).
  See `docs/API.md` — the canonical contract shared with the mobile repo.

The Thymeleaf website and the REST API MUST share the same service layer.
Never fork business logic between a controller and a template.

## Runtime

- Java 21 (LTS)
- Spring Boot 3.5.x — `web`, `thymeleaf`, `data-jpa`, `validation`,
  `mail`, `security`, `oauth2-client`, `actuator`
- PostgreSQL 16 in prod/local; H2 for tests
- Flyway migrations (`src/main/resources/db/migration/V*.sql`, immutable)
- Maven wrapper (`./mvnw`)

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication      main
├── activation                     drip campaigns (activation, trial-end)
├── admin                          /admin/revenue dashboard + churn
├── auth                           form + OAuth2 login, sessions, throttling
├── billing                        Stripe checkout, portal, webhook
├── digest                         weekly digest email + opt-out
├── domain                         JPA entities
├── email                          IMAP poll, IDLE, Gmail OAuth, SMTP send
├── reengagement                   dormant-user emails
├── repository                     Spring Data repositories
├── service                        conversation, threading, IM transform
├── team                           team model + invites
└── web                            Thymeleaf controllers, chat, mailboxes
```

Configuration:

```
src/main/resources/
├── application.yml                base (reads JDBC_* env vars)
├── application-local.yml          local profile (localhost Postgres)
├── db/migration/                  Flyway V*.sql (immutable once merged)
├── legal/                         Markdown for /privacy, /terms, /refund
├── static/                        css, js, images, PWA icons, sw.js
└── templates/                     Thymeleaf views
```

## Domain overview

Core entities (see `com.emailmessenger.domain`):

- `User` — email + password (or Google subject), plan, activation flags.
- `Subscription` — plan, Stripe ids, billing period, cancellation reason.
- `Team`, `TeamMember`, `TeamInvite` — team plan sharing.
- `MailAccount` — connected mailbox (IMAP password or Gmail OAuth), last
  seen UIDs, polling policy, signature.
- `EmailThread` — one conversation, keyed by `conversationKey` (a
  normalized participant-set — see V32 migration and
  `ConversationKeyService`), owned by a `User`.
- `Message` — one email in a thread (headers, plain + html body,
  outbound flag).
- `MessageRecipient` — to/cc/bcc rows joined to a `Participant`.
- `Participant` — deduped by normalized email address.
- `Attachment` — filename, mime, size; `blob_ref` still unused today.
- `SavedSearch` — user-defined filters with unread counts.
- `PasswordResetToken`, `EmailVerificationToken`, `AuthEvent` — auth ops.
- `PlanChangeEvent` — audit trail for plan up/downgrades.

## Auth model

- **Form login** at `/login` (session cookie + remember-me via
  `JdbcTokenRepositoryImpl`).
- **Google OIDC** at `/auth/google/start` (provisioning by Google
  subject in `OAuth2ProvisioningService`).
- **Throttling** via `LoginThrottleFilter` before the form-login filter.
- **Verification** via emailed link (`EmailVerificationController`).
- **Password reset** via emailed link (`PasswordResetController`).
- CSRF is enabled by default (Spring Security 6). The Stripe webhook
  and the mobile REST API are the only stateless callers — see
  `docs/API.md` for how the mobile client authenticates.

## Email import

- `MailboxPollingService` — scheduled IMAP poll pulling new UIDs into
  `EmailThread` / `Message` / `Participant`.
- `ImapIdleService` — long-lived IMAP IDLE for paid tiers
  (`mailbox.idle.enabled` flag).
- Sent-folder sync uses `mail_account_last_seen_sent_uid` (V31) so
  outbound Delivered state can eventually be inferred (roadmap item).
- Gmail OAuth mailboxes: `GmailConnectController`,
  `GoogleOidcUserService`, refresh via `/mailboxes/gmail/reconnect/{id}`.

## IM transform

- `EmailBodyCleaner` — strips quoted replies, "On … wrote:", trims
  signatures into structured fields.
- `IMTransformService` — collapses consecutive same-sender messages
  into bubble runs; enriches with day separators.
- `ConversationService` — orchestrates thread → view model.
- `ConversationKeyService` — normalized participant-set key for the
  chats-style workspace.

## Live delivery

- `ChatEventStream` — SSE stream at `/chats/events` for the open web
  workspace. `NewMailEvent` is published by `MailboxPollingService` /
  `ImapIdleService` and rendered in-place by `fragments/live-refresh.html`.
- Web Push (VAPID / service worker) is roadmap; not implemented.

## Billing

- Stripe checkout via `PlanCheckoutSuccessHandler`.
- `BillingWebhookController` — the ONE `@RestController` in the codebase;
  everything else is Thymeleaf.
- `SubscriptionService` holds plan state; `PlanLimitKind` caps free-tier
  usage.

## Admin & telemetry

- `/admin/revenue` — churn card, at-risk queue, per-plan MRR
  (`AdminRevenueController`, `ADMIN_EMAILS` gate).
- Weekly digest to operators (`ADMIN_WEEKLY_DIGEST_ENABLED`).
- Activation drip + trial-end drip: `ActivationService`,
  `TrialEndConversionService`, both flag-gated for live sends.

## Related repositories

- **`conexus-mail-mobile`** — Flutter mobile client. Consumes the REST
  API described in `docs/API.md`. Its `lib/domain/` mirrors this
  backend's business rules; the server is authoritative and always wins
  on conflict. See its `docs/DESIGN.md` for product design and roadmap.

## Contract with the mobile client

- `docs/API.md` in **this** repo is the single source of truth for the
  REST API. Both repos read from and write to it in the same commit.
- The mobile client never talks to the database directly. All state
  mutations round-trip through the REST API in a single transaction.
- Client-supplied values for anything the server can compute (plan,
  quotas, timestamps, conversationKey) are ignored; the server
  recomputes.
