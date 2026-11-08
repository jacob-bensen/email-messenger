# BACKEND — architecture (reverse-engineered 2026-11-08)

Repo role: **BACKEND + website** (Spring Boot 3, Java 21, Maven, JPA/Hibernate, Flyway, PostgreSQL 16; H2 in tests).
Note: the routine template assumes jOOQ; this repo does **not** use jOOQ — persistence is Spring Data JPA (`domain/`, `repository/`). Codegen is N/A.

## Packages (`com.emailmessenger`)
| Package | Responsibility |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, Gmail connect, saved searches, pricing/legal/SEO/PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, `ChatEventStream` (SSE) |
| `service` | Conversation building: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService` (participant-set key), `ReplyService` (SMTP send), view-model records (`ChatConversation`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`) |
| `email` | IMAP/Gmail import: `MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor` |
| `auth` | Spring Security config, registration, Google OIDC, password reset, email verification, login throttling, account pages |
| `billing` | Stripe checkout/portal/webhook, plans & limits (`PlanLimitService`), trial nudges |
| `team` | Team invites/membership |
| `digest`, `activation`, `reengagement`, `admin` | Scheduled emails and admin revenue/retention metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (tables, from Flyway V1–V35)
users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients, participants, attachments (`blob_ref` currently always null), thread_notes, saved_searches, teams, team_members, team_invites, digest_email_preferences, auth_events, email_verifications, password_reset_tokens, persistent_logins (remember-me).
Conversations are not a table: they are a computed grouping by `conversationKey` (participant set) over threads/messages.

## Auth model
- Browser session (JSESSIONID) via form login `POST /login` (`email`, `password`) or Google OIDC; remember-me cookie (30 days, DB-backed).
- CSRF **enabled** except `/h2-console/**` and `/billing/webhook`.
- Everything not on the public allow-list (see `SecurityConfig`) requires authentication.
- **No token/bearer auth exists yet** — blocker for a mobile client (see INTERNAL_TODO).

## Web vs REST
There is currently **no REST API**. The only non-HTML endpoints are `POST /chats/refresh` (JSON `{"imported":n}`), `GET /chats/events` (SSE), and `POST /billing/webhook` (Stripe). See `docs/API.md` and `docs/WEB.md`. Business logic lives in services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`); controllers are thin-ish but `ConversationController` still holds some logic (recipient parsing, sending-account selection, signature append) that must move to a service before a REST controller can reuse it.
