# BACKEND

Reverse-engineered from the code on 2026-10-19. Anything not verifiable in
code is marked **TBD**.

**Repo classification:** `email-messenger` = BACKEND (pom.xml, Spring Boot,
Flyway). Its sibling `conexus-mail-mobile` = FRONTEND (Flutter; currently
only a README, so classified by name + `.gitignore` + README "Flutter
application for ConexusMail" — no `pubspec.yaml` yet).

## Stack (actual — differs from the sync routine's assumptions)

- Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (there is **no jOOQ**
  and no codegen step), Flyway, PostgreSQL (H2 in tests), Maven wrapper.
- Thymeleaf server-rendered web app; Spring Security (form login + optional
  Google OIDC, remember-me, CSRF on).
- Stripe for billing, Jakarta Mail for IMAP/SMTP, Gmail OAuth for mailboxes.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, Gmail connect, saved searches, legal, PWA, SEO, demo), `ChatEventStream` SSE hub |
| `service` | IM transform: `ChatService`/`ConversationService`, `ConversationListService`, `ConversationKeyService`, `ReplyService`, `OutboundMessageService`, `EmailBodyCleaner`, view records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`, ...) |
| `email` | IMAP/SMTP: `MailAccountService`, `MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor` |
| `auth` | Registration, login, throttling, password reset, email verification, Google OIDC, account settings |
| `billing` | Plans/limits (`PlanLimits`), Stripe checkout/portal/webhook, trial conversion |
| `team` | Team invites |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |
| `admin` | `/admin/revenue` metrics + win-back outreach |
| `domain`, `repository` | JPA entities / Spring Data repos |

## Data model (Flyway V1–V35, immutable)

Tables: `users`, `persistent_logins`, `subscriptions`, `plan_change_events`,
`mail_accounts`, `participants`, `email_threads` (+ `conversation_key`, V32),
`messages` (+ outbound flag, V30), `message_recipients`, `attachments`
(`blob_ref` always null today), `saved_searches`, `digest_email_preferences`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `teams`,
`team_members`, `team_invites`. (`thread_notes` created V25, dropped V33.)

Core concept: a **conversation** is keyed by the participant set
(`conversationKey`), merged across email threads into one timeline.

## Auth model

- Session cookie (`JSESSIONID`) via form login `POST /login`
  (`email`, `password`, optional `remember-me`; CSRF token required) or Google
  OIDC (`/auth/google/start`). Remember-me: 30 days, persistent tokens.
- Everything outside the permit-list in `SecurityConfig` requires
  authentication; CSRF is enforced except `/billing/webhook` and `/h2-console/**`.
- **There is no token/Bearer auth.** A mobile client cannot authenticate
  without a new mechanism (see `INTERNAL_TODO.md`).

## Web vs. REST

The app is almost entirely server-rendered HTML; controllers are `@Controller`
returning view names / redirects. Exceptions returning data:
`POST /chats/refresh` (JSON), `GET /chats/events` (SSE),
`POST /billing/webhook` (`@RestController`). Business logic lives in services;
controllers parse params, call services, populate models. Details: `API.md`,
`WEB.md`.

## Plans

`FREE` (3 mailboxes, 500 threads), `PRO` (5 mailboxes, unlimited history),
`BUSINESS` (unbounded) — `billing/PlanLimits`. Saved searches unlimited.

## Related operational docs

`master/` (PLAN, BACKLOG, CHANGELOG, MASTER_ACTIONS) belongs to a separate
product-planning routine. The root `CHANGELOG.md` / `INTERNAL_TODO.md` belong
to the full-stack sync routine.
