# BACKEND.md — email-messenger

Server for the ConexusMail product. This repo owns all persistence, all
business logic, the Thymeleaf website, and (once it exists) the REST API
consumed by the mobile client at `conexus-mail-mobile`.

> This is a reverse-engineered snapshot as of 2026-08-30. Anything marked
> `TBD` was not verifiable from the code at that time.

## Stack (actual)

- Java 21, Spring Boot 3.5.x, Maven with wrapper.
- Persistence: **Spring Data JPA** (not jOOQ). PostgreSQL 16 in prod/dev,
  H2 for tests.
- Migrations: Flyway. `src/main/resources/db/migration/V1__…` through
  `V35__mail_account_signature.sql` at time of writing.
- Views: Thymeleaf, server-rendered.
- Auth: Spring Security — form login + Google OIDC. See
  `com.emailmessenger.auth.SecurityConfig`.
- Mail: Jakarta Mail via `spring-boot-starter-mail`; custom IMAP polling
  and IDLE.
- Billing: Stripe (Checkout + Customer Portal + webhook at
  `/billing/webhook`).
- Real-time: SSE at `/chats/events`.
- PWA shell served by `PwaController`.

> The umbrella routine assumes jOOQ. This project uses JPA instead;
> follow the JPA idiom already in the codebase — do not introduce jOOQ.

## Package layout

```
com.emailmessenger
├── EmailMessengerApplication.java
├── admin/         # AdminRevenueController + retention operator tooling
├── activation/    # ActivationService — day-N onboarding drip (scheduled)
├── auth/          # SecurityConfig, form login, Google OAuth, account,
│                    email verification, password reset
├── billing/       # Stripe checkout, portal, webhook, plan model
├── digest/        # weekly digest, opt-out endpoint
├── domain/        # JPA entities (EmailThread, Message, Participant,
│                    Attachment, MailAccount, User, Subscription, Team …)
├── email/         # IMAP polling, IMAP IDLE, SMTP send, DSN parsing
├── reengagement/  # trial-end and win-back services
├── repository/    # Spring Data repositories
├── service/       # threading, chat/conversation transform, cleaner, …
├── team/          # team invites
└── web/           # Thymeleaf controllers: /chats, mailboxes, pricing,
                    legal, PWA assets, SSE, demo, saved searches
```

## Key controllers → services → tables

| Feature                | Controller                       | Primary service(s)                        | Main tables                             |
|------------------------|----------------------------------|-------------------------------------------|-----------------------------------------|
| Auth / accounts        | `AuthController`, `AccountController`, `EmailVerificationController`, `PasswordResetController` | user + auth services | `users`, `password_resets` (see V2)     |
| Google sign-in         | `OAuthStartController` + Spring OAuth2 client | — | `users`                                 |
| Chats workspace        | `ConversationController` (also serves `/chats`) | conversation / IM transform services | `email_thread`, `message`, `participant`, `attachment` |
| Chats live updates     | `ChatEventsController` (SSE)     | `ChatEventStream`, `NewMailEvent`         | (in-memory pub/sub)                     |
| Mailbox connect (IMAP) | `MailboxController`              | IMAP client / encryption                  | `mail_account` (V5, V29 auth_type)      |
| Gmail OAuth mailbox    | `GmailConnectController`         | Google token exchange                     | `mail_account`                          |
| Pricing / billing      | `PricingController`, `BillingController`, `BillingWebhookController` | Stripe client, plan model | `subscription`, `plan_change_events`    |
| Saved searches         | `SavedSearchController`          | —                                         | `saved_search` (TBD)                    |
| Team invites           | `TeamInviteController`           | team invite service                       | `teams`, `team_invite` (V24)            |
| Digest opt-out         | `DigestController`               | digest service                            | `digest_optout` (TBD)                   |
| Admin revenue          | `AdminRevenueController`         | admin revenue service                     | reads across billing tables             |
| PWA shell / SEO        | `PwaController`, `SeoController` | —                                         | —                                       |
| Demo / marketing       | `DemoController`, `LegalController` | —                                       | —                                       |

## Auth model

- Form login (`/login`) with BCrypt passwords in `users`.
- Google OIDC ("Continue with Google") when
  `AUTH_GOOGLE_CLIENT_ID` / `AUTH_GOOGLE_CLIENT_SECRET` are set.
- CSRF protected everywhere (Spring Security default). All non-GET routes
  currently expect a form CSRF token.
- Admin gate: `ADMIN_EMAILS` env var — comma-separated allowlist for
  `/admin/**`.
- Every controller reads the current user via `Authentication` / a
  principal-lookup pattern in the auth package — no bearer tokens today.
  See §"Mobile client → server auth" in `docs/API.md` for the plan.

## Real-time architecture

- `MailboxPollingService` runs on `@Scheduled`; publishes `NewMailEvent`
  on the Spring event bus after each import.
- For paid tiers `ImapIdleService` keeps IMAP IDLE connections open
  (gated by `mailbox.idle.enabled`).
- `ChatEventStream` bridges those events to per-user SSE emitters at
  `/chats/events`; the browser HTML morphs the rail and open pane in
  place via `templates/fragments/live-refresh.html`.

## Website ↔ services

Thymeleaf pages and (future) API endpoints MUST share the same service
layer. Controllers only marshal input/output. See `docs/WEB.md` for the
page inventory that pairs with this file.

## Config & secrets

- `application.yml` (base): reads `JDBC_DATABASE_URL`,
  `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD` + Stripe/OAuth env
  vars.
- `application-local.yml` for local dev (Postgres at
  `localhost:5432/email_messenger`).
- `src/test/resources/application.yml` for tests (H2).
- Bootstrap the local DB with `db/init.sql`.
- Human-blocked deploy items live in `master/MASTER_ACTIONS.md`.

## Roadmap

The product roadmap lives in `CLAUDE.md`. The mobile-facing roadmap
(what the phone app should do) lives in
`conexus-mail-mobile/docs/DESIGN.md`.
