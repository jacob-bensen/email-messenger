# Backend architecture

Reverse-engineered 2026-11-08 from the code. Anything not verified is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3 (web, thymeleaf, security, oauth2-client, data-jpa,
validation, mail, actuator), **Spring Data JPA/Hibernate** (NOT jOOQ — no codegen
step), Flyway (`src/main/resources/db/migration`, V1..V35), PostgreSQL 16
(H2 in tests), Stripe Java SDK, Jakarta Mail (IMAP/SMTP), jsoup, GreenMail +
Testcontainers in tests. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
- `web/` Thymeleaf controllers (chats, mailboxes, saved searches, pricing, legal,
  SEO, PWA), SSE stream `/chats/events`, `GlobalExceptionHandler`.
- `service/` IM transform (`IMTransformService`, `EmailBodyCleaner`),
  `ConversationKeyService` (participant-set key), `ReplyService` (SMTP send).
- `email/` mailbox integration: IMAP polling/IDLE, Gmail OAuth, MIME parsing,
  import, credential encryption.
- `auth/` form login + Google OIDC, registration, password reset, email
  verification, login throttling, account settings.
- `billing/` Stripe checkout/portal/webhook, plan limits (Free/Pro/Business).
- `team/`, `digest/`, `admin/`, `activation/`, `reengagement/` — teams/invites,
  digests, operator metrics (`/admin/**`), lifecycle schedulers (off unless flagged).
- `domain/` JPA entities, `repository/` Spring Data repositories.

## Data model (tables)
users, persistent_logins, subscriptions, plan_change_events, mail_accounts,
participants, email_threads, messages, message_recipients, attachments
(`blob_ref` always null — bytes are not stored), thread_notes, saved_searches,
teams, team_members, team_invites, digest_email_preferences,
password_reset_tokens, email_verifications, auth_events.
Conversations in the UI are keyed by `conversationKey` (participant set), which
spans multiple `email_threads`.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (persistent token, 30d). Form login
at `POST /login` (`email`, `password`), optional Google OIDC (`/auth/google/start`).
CSRF is ON for everything except `/billing/webhook` and `/h2-console/**`.
Everything not in the permitAll list (`SecurityConfig`) requires login.
**There is no token/API-key auth.** `/admin/**` is allowlisted by email
(`admin.emails`) and 404s for others.

## Web vs REST
All endpoints today are server-rendered web routes (see `WEB.md`) except a few
JSON/SSE helpers (`POST /chats/refresh`, `GET /chats/events`). A mobile REST API
does **not exist yet**; see `API.md` for the status and the proposed plan.
Controllers delegate to services (`ChatService`, `ConversationListService`,
`MailAccountService`, `ReplyService`, `OutboundMessageService`); a future
`/api/v1` layer must reuse these, not fork logic.
