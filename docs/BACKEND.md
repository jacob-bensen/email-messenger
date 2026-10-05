# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-05. Anything not verified is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (not jOOQ), PostgreSQL (H2 in tests),
Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security, Maven wrapper.
Stripe (billing), Jakarta Mail (IMAP/SMTP), Google OAuth (login + Gmail mailbox connect).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf `@Controller`s (chats, mailboxes, saved searches, pricing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`; view records `ChatConversation`, `ConversationListItem`, `BubbleMessage`, `BubbleRun` |
| `domain` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, …) |
| `repository` | Spring Data repositories |
| `email` | IMAP polling/IDLE, import (`EmailImportService`, `MimeMessageParser`), Gmail OAuth, credential encryption |
| `auth` | Security config, registration, login throttle, password reset, email verification, Google OIDC, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digest, lifecycle emails, operator revenue metrics |

## Schema overview (Flyway)
users, subscriptions, plan_change_events, mail_accounts (IMAP password or Gmail OAuth,
encrypted creds, last-seen UIDs), participants, email_threads (owner + `conversation_key`),
messages (incl. outbound flag), message_recipients, attachments (`blob_ref` always null today),
saved_searches, digest_email_preferences, password_reset_tokens, email_verifications,
auth_events, teams/team_members/team_invites, persistent_logins.
`thread_notes` was created in V25 and dropped in V33.

## Auth model
Server session (cookie) via form login (`/login`) or Google OIDC (only registered if
`GOOGLE_CLIENT_ID` is set). CSRF is **enabled** (exempt: `/h2-console/**`, `/billing/webhook`).
Remember-me via `persistent_logins`. Login throttling filter precedes form login.
Public routes are whitelisted in `SecurityConfig`; everything else requires authentication.
Plans: `FREE`, `PRO`, `BUSINESS` (`domain/Plan`); limits in `billing/PlanLimits`.

## Web vs REST
There is **no REST/JSON API**. The only non-HTML endpoints are `GET /chats/events` (SSE,
new-mail signal), `POST /billing/webhook` (Stripe), PWA manifest/sw/icons, robots/sitemap.
All web controllers delegate to services; the future `/api/v1` MUST reuse those same
services (`ChatService`, `ConversationListService`, `OutboundMessageService`,
`MailAccountService`, `PlanLimitService`) — see `API.md`.
