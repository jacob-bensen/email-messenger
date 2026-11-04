# BACKEND.md — architecture

Reverse-engineered 2026-11-04 from the code. Anything not verifiable is marked **TBD**.

## Stack (actual)
Java 21, Spring Boot 3.5, Spring MVC + Thymeleaf, **Spring Data JPA/Hibernate** (not jOOQ — there is no jOOQ
codegen), PostgreSQL 16 (H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Spring Security
(form login + Google OIDC + remember-me), Stripe (`stripe-java`), Jakarta Mail (IMAP/SMTP), jsoup, GreenMail
and Testcontainers for tests. Maven wrapper (`./mvnw`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, Gmail connect, pricing, legal, PWA, SEO, saved searches), `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` (SSE) |
| `service` | Conversation/IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records `ChatConversation`, `BubbleRun`, `BubbleMessage`, `ConversationListItem` |
| `email` | Mailbox integration: IMAP polling (`MailboxPollingService/Scheduler`), IMAP IDLE (`ImapIdleService`), Gmail OAuth, MIME parsing, `EmailImportService`, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, tokens, audit events) |
| `auth` | Security config, registration, login throttling, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, onboarding nudges, win-back, admin revenue/retention metrics |

## Schema overview (tables)
users, mail_accounts, email_threads (has `conversation_key`, per-user unread), messages, message_recipients,
participants, attachments (`blob_ref` always null: bytes are not stored), subscriptions, plan_change_events,
teams, team_members, team_invites, saved_searches, digest_email_preferences, email_verifications,
password_reset_tokens, persistent_logins, auth_events.

## Auth model
Browser **session cookie** (JSESSIONID) + optional remember-me (30 d). CSRF is enabled for every route except
`/billing/webhook` and `/h2-console/**`. Everything outside the public allow-list in `SecurityConfig` requires
authentication. **There is no token-based (bearer/JWT) auth, so a mobile client cannot authenticate yet.**

## Web vs REST
The app is entirely server-rendered. The only JSON endpoints are `POST /chats/refresh` and the Stripe webhook;
`/chats/events` is an SSE stream. There is **no REST API for the mobile client** (see `API.md`, `INTERNAL_TODO.md`).
Controllers delegate to services (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`MailAccountService`, `PlanLimitService`). New `/api/v1` controllers must reuse those services.

## Key concept: conversationKey
A "chat" is the participant set (excluding the owner) merged across email threads; identified by
`conversationKey` (`ConversationKeyService`). Scope = one mailbox address, or all (Hub/Dashboard).
