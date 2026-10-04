# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-04. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring Data **JPA** (not jOOQ — no codegen step exists), PostgreSQL 16
(H2 in tests), Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security,
Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, saved searches, landing/pricing/legal, PWA, SSE (`ChatEventStream`) |
| `service` | Conversation model: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`; view records `ChatConversation`, `BubbleRun`, `BubbleMessage`, ... |
| `email` | IMAP/SMTP: import (`EmailImportService`, `MimeMessageParser`), polling, IDLE, Gmail OAuth, credential encryption |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Data model (tables)
users, persistent_logins, subscriptions, plan_change_events, auth_events, password_reset_tokens,
email_verifications, mail_accounts, participants, email_threads, messages, message_recipients,
attachments (`blob_ref` always null — bytes are not stored), saved_searches, digest_email_preferences,
teams, team_members, team_invites, thread_notes.
A *conversation* is not a table: it is `conversationKey` (hash of the participant set) over
`email_threads`, merged into one timeline by `ConversationService`/`ChatService`.

## Auth model
Browser session cookie (`JSESSIONID`) + remember-me (30 d, persistent token table). Form login at
`POST /login` (`email`, `password`), optional Google OIDC (`/auth/google/start`). CSRF is ON
(exempt: `/billing/webhook`, `/h2-console/**`). All routes except an allow-list in
`auth/SecurityConfig` require authentication. **There is no token/bearer auth, so a mobile
client cannot authenticate non-interactively today** (see INTERNAL_TODO).

## Web vs REST
Everything is server-rendered. The only JSON/stream endpoints are `POST /chats/refresh`,
`GET /chats/events` (SSE) and the Stripe webhook. Controllers delegate to services
(`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`), which
is the layer a REST API must reuse. Some orchestration lives in `ConversationController`
(mailbox scoping, signature append, new-vs-reply decision) — see INTERNAL_TODO.
