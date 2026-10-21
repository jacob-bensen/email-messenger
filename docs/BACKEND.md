# Backend architecture

Reverse-engineered from the code on 2026-10-21. Unknowns are marked TBD.

## Stack (actual)

Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (not jOOQ — there is no
jOOQ codegen), PostgreSQL 16 (prod/local), H2 (tests), Flyway (`V1`..`V35`),
Maven, Thymeleaf. Stripe for billing; Jakarta Mail for IMAP/SMTP; Gmail OAuth.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, marketing pages, PWA, SSE) and web-facing services (`ChatService`, `ConversationListService`, `OutboundMessageService`, onboarding) |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`, …) |
| `email` | IMAP/Gmail integration: import, polling (`MailboxPollingService`, scheduler), IMAP IDLE, credential encryption, MIME parsing |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttling, remember-me |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial conversion |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest email, activation nudges, win-back, admin metrics |
| `domain` | JPA entities |
| `repository` | Spring Data repositories |

## Data model (Flyway tables)

users, mail_accounts, email_threads, messages, message_recipients, participants,
attachments (`blob_ref` always null today), saved_searches, subscriptions,
plan_change_events, teams, team_members, team_invites, auth_events,
email_verifications, password_reset_tokens, persistent_logins,
digest_email_preferences. (`thread_notes` dropped in V33.) A conversation is a
view over threads keyed by the participant set (`conversationKey`), not a table.

## Auth model

Session cookie (form login or Google OIDC), CSRF enabled (ignored only for
`/h2-console/**` and `/billing/webhook`), remember-me via `persistent_logins`.
All routes except the public allow-list in `auth/SecurityConfig` require login.
**There is no token / API-key auth** — a mobile client cannot authenticate
today without scraping the form login. See `INTERNAL_TODO.md`.

## Web vs REST

The app is server-rendered only. The sole JSON/stream endpoints are
`POST /chats/refresh`, `GET /chats/events` (SSE) and `POST /billing/webhook`.
There is no `/api/**` surface. Controllers delegate to services; mobile-facing
JSON should reuse `ChatService` / `ConversationListService` / `ReplyService` /
`OutboundMessageService` so web and API cannot diverge.
