# BACKEND.md — ConexusMail server architecture

Reverse-engineered from the code on 2026-10-07. Anything not implemented is marked **TBD**.

## Classification
`email-messenger` is the BACKEND: `pom.xml` + Spring Boot + Flyway migrations (`src/main/resources/db/migration`).
Sibling `conexus-mail-mobile` is the FRONTEND (Flutter; currently only a README).

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA/Hibernate (not jOOQ)**, PostgreSQL 16 (H2 in tests), Flyway (V1–V35),
Thymeleaf, Spring Security (form login + Google OIDC + persistent remember-me), Jakarta Mail (IMAP/SMTP),
Stripe (`stripe-java`), jsoup. Maven wrapper. No REST/JSON API for a mobile client exists yet (see API.md).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, PWA/SEO/legal/demo), plus `ChatService`, `ConversationListService`, `ChatEventStream` (SSE) |
| `service` | Mail-to-chat transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` (SMTP send), view records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`, `SenderBubbleRun`, `ChatMember`) |
| `email` | IMAP import/poll/IDLE (`MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`), Gmail OAuth client, credential encryption |
| `domain` / `repository` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Team*`, `SavedSearch`, `Subscription`, …) and Spring Data repos |
| `auth` | Security config, registration, login throttle, password reset, email verification, account settings, Google sign-in |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`), banners, trial nudges |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, weekly digest, operator metrics, lifecycle email schedulers |

## Key concepts
- **conversationKey**: stable key derived from the participant set (excluding owner addresses); one chat per key, merged across email threads. Pure server logic in `ConversationKeyService`.
- **Plans**: FREE / PRO / BUSINESS. FREE: 3 mailboxes, 500 threads; PRO: 5 mailboxes, unlimited history; BUSINESS unlimited (`PlanLimits`). Server enforces (`PlanLimitService`).
- **Mailbox scope**: `mailbox` param scopes lists to one connected account; absent = cross-account "Hub".
- **Live delivery**: poll (`MailboxPollingScheduler`) + IMAP IDLE for paid tiers → `NewMailEvent` → `ChatEventStream` SSE at `/chats/events`.

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, fields `email`, `password`, optional `remember-me`) or Google OIDC. CSRF **enabled** (only `/billing/webhook`, `/h2-console/**` exempt). Everything except the public list in `SecurityConfig` requires authentication. **No token (Bearer/JWT) auth exists** — required for mobile; TBD.

## Web ↔ API relationship
Today the website is the only client; controllers call services directly (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`). A JSON API must reuse those services; not yet built. Some orchestration lives in controllers and would need extraction first (see INTERNAL_TODO.md).

## Schema overview
See Flyway files. Core: users, mail_accounts, email_threads, messages, message_recipients, participants, attachments (`blob_ref` always null — bytes not stored), saved_searches, subscriptions, plan_change_events, teams/team_members/team_invites, auth_events, tokens, persistent_logins.
