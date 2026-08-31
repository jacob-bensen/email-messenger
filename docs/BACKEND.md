# Backend architecture — ConexusMail (`email-messenger`)

This is the authoritative Spring Boot app. It owns:

- All persistence (PostgreSQL 16, JPA / Hibernate, Flyway migrations).
- All business logic (email import, threading, IM transform, billing, auth).
- The public marketing site + full user-facing web app, rendered server-side
  with Thymeleaf.
- (Planned) a REST API layer for the mobile client `conexus-mail-mobile`.
  **This layer does not yet exist** — see `docs/API.md` for the target
  contract and `INTERNAL_TODO.md` for the sync gap.

## Stack

| Layer          | Choice                                           |
| -------------- | ------------------------------------------------ |
| Language       | Java 21 (LTS)                                    |
| Framework      | Spring Boot 3.x                                  |
| Persistence    | Spring Data JPA / Hibernate (not jOOQ)           |
| Migrations     | Flyway (`src/main/resources/db/migration`)       |
| DB (prod/dev)  | PostgreSQL 16                                    |
| DB (test)      | H2                                               |
| Templating     | Thymeleaf                                        |
| Build          | Maven wrapper (`./mvnw`)                         |
| Tests          | JUnit 5, Spring Boot Test, Testcontainers, GreenMail |

## Package layout (`com.emailmessenger`)

```
EmailMessengerApplication          # Spring Boot entry point
web/          # Thymeleaf controllers: chats, conversations, mailboxes,
              # onboarding, PWA, SEO, pricing, legal, demo, chat SSE stream
auth/         # Registration, login, Google OIDC, email verification,
              # password reset, account settings, auth events audit
billing/      # Stripe checkout / portal / webhook, Plan (FREE/PRO/BUSINESS),
              # BillingPeriod, plan-change events, cancellation reasons
domain/       # JPA entities — see "Domain model" below
repository/   # Spring Data repositories
service/      # ConversationService, IMTransformService, ReplyService,
              # EmailBodyCleaner, ConversationKeyService, demo data, etc.
email/        # IMAP polling + IDLE, Gmail OAuth, MIME parsing, credential
              # encryption, sent-folder cursor, NewMailEvent publishing
digest/       # Daily/weekly digest emails and opt-out
activation/   # New-user activation nudges + follow-ups
reengagement/ # Win-back tracking for lapsed users
team/         # Teams, team members, team invites
admin/        # Admin revenue dashboard, retention tools
```

## Domain model

Entities live in `com.emailmessenger.domain`; each maps to a Flyway-managed
table.

- `User` — the account holder. Owns mailboxes, threads, saved searches, a
  subscription, a team.
- `MailAccount` — a connected mailbox (IMAP + password or Gmail OAuth). Holds
  encrypted credentials, polling policy, last-seen UID cursors for INBOX and
  Sent, optional signature.
- `EmailThread` — a group of messages tied together by
  `Message-ID` / `In-Reply-To` / `References`, owned by a `User`. Has a
  `conversationKey` (SHA of sorted, lowercased non-owner participants) which
  groups threads into a *chat* — the texting-style view collapses all threads
  that share a `conversationKey` into one conversation.
- `Message` — one email. Records `messageIdHeader`, `inReplyTo`, sender
  `Participant`, plain + HTML bodies, `sentAt`, `outbound` (true if the user
  sent it from ConexusMail, false if imported over IMAP), and attachments.
- `MessageRecipient` — to/cc/bcc participants for a `Message`
  (`RecipientType`).
- `Participant` — deduped person, keyed by normalized email address; carries
  display name and avatar hints.
- `Attachment` — filename, MIME, size, and a `blob_ref` (currently always
  null — bytes are not yet stored; see roadmap).
- `Subscription`, `Plan`, `BillingPeriod`, `PlanChangeEvent`,
  `CancellationReason` — Stripe billing state.
- `SavedSearch` — persisted user query.
- `AuthEvent` / `AuthEventType` — auth audit log.
- `EmailVerificationToken`, `PasswordResetToken` — single-use tokens.
- `Team`, `TeamMember`, `TeamMemberRole`, `TeamInvite` — teams feature.
- `DigestEmailPreference` — per-user digest cadence.

## Key services (`com.emailmessenger.service`)

- `ConversationService` — builds the merged chat view for a `conversationKey`:
  loads all threads that share the key, orders by `sentAt`, hands off to
  `IMTransformService`.
- `IMTransformService` — strips quoted replies, collapses consecutive
  same-sender messages into `SenderBubbleRun`s, renders markdown-lite so
  bubbles look like iMessage.
- `EmailBodyCleaner` — quote stripping, signature extraction into structured
  fields (phone, title, company, sites).
- `ReplyService` — composes and sends an outbound reply through the account's
  provider; writes an outbound `Message` back to the thread.
- `ConversationKeyService` — computes/refreshes the participant-set hash.
- `DemoConversationService` — canned data for the `/demo` page.

## Email pipeline (`com.emailmessenger.email`)

1. `MailboxPollingScheduler` triggers `MailboxPollingService` on a cadence
   set by `PollingPolicy`.
2. `JakartaImapClient` (or `GmailOAuthClient` for Gmail OAuth accounts)
   fetches new messages by UID since the account's last-seen cursor —
   separate cursors for INBOX and Sent.
3. `MimeMessageParser` turns each MIME message into a `ParsedEmail`.
4. `EmailImportService` upserts `EmailThread` / `Message` / `Participant` /
   `MessageRecipient` / `Attachment`, then bumps `conversationKey` via
   `ConversationKeyService` and publishes a `NewMailEvent`.
5. For paid-tier accounts, `ImapIdleService` holds an IDLE connection per
   account (behind `mailbox.idle.enabled`) so new mail lands immediately;
   the same `NewMailEvent` fires.
6. `ChatEventStream` fans `NewMailEvent` out over an SSE endpoint at
   `/chats/events`; the chat UI morphs its rail and the open pane in place
   via `fragments/live-refresh.html`.

## Auth model

- Form login (`email` + password) with BCrypt.
- Google OIDC via `OAuthStartController` and the `/callback` endpoint.
- Email verification is required before certain actions; tokens live in
  `email_verifications`. Password resets use `password_reset_tokens`.
- `AuthEvent` writes an audit row for every login / register / reset /
  verify (`AuthEventType`).
- Sessions are Spring Security's default (server-side).

## Web + REST relationship (very important)

Today the app is Thymeleaf-only. When the REST API for the mobile client is
introduced (see `docs/API.md`), every state mutation must go through the
same service beans as the web controllers — no forked logic in
`@RestController`s or in templates. Web controllers and future REST
controllers both call into `service/`, `email/`, `billing/`, etc.

## Configuration

- Base `application.yml` reads `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`,
  `JDBC_DATABASE_PASSWORD` from env.
- `application-local.yml` (`local` profile) points at
  `jdbc:postgresql://localhost:5432/email_messenger`; bootstrap the DB with
  `db/init.sql`.
- Test resources use H2.
- Feature flag `mailbox.idle.enabled` gates IMAP IDLE for paid tiers.

## Related repositories

See each repo's `CLAUDE.md` "Related repositories" section for the sibling.
The mobile client mirrors this backend's rules; the API contract in
`docs/API.md` is the shared source of truth.
