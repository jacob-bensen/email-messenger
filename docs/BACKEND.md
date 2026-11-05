# BACKEND — architecture (reverse-engineered 2026-11-05)

Spring Boot 3 / Java 21 / Maven monolith, server-rendered with Thymeleaf. Repo classification
evidence: `pom.xml` + Spring annotations + 35 Flyway migrations. Deviations from the generic
"target stack": persistence is **Spring Data JPA/Hibernate (not jOOQ)**, `ddl-auto: validate`; no
jOOQ codegen exists.

## Packages (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `domain` | JPA entities: User, Subscription, MailAccount, EmailThread, Message, MessageRecipient, Participant, Attachment, Team*, SavedSearch, AuthEvent, tokens, PlanChangeEvent |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling (`MailboxPollingService`, scheduler), IMAP IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ConversationKeyService`, `ReplyService` (SMTP send) |
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace, `MailboxController`, `GmailConnectController`, PWA, SEO, legal, pricing, SSE `ChatEventsController`) |
| `auth` | Spring Security config, form login + Google OIDC, registration, password reset, email verification, throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, weekly digest, lifecycle emails, admin metrics |

## Data model (Flyway V1–V35, tables)
users, persistent_logins, subscriptions, mail_accounts (auth_type password|oauth, last_seen_uid,
last_seen_sent_uid, signature), email_threads (owner, unread, conversation_key), messages
(outbound flag), message_recipients, participants, attachments (`blob_ref` always null — bytes are
not stored), saved_searches, digest_email_preferences, password_reset_tokens, email_verifications,
auth_events, teams, team_members, team_invites, plan_change_events. `thread_notes` created V25,
dropped V33.

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,`password`) or Google
OIDC (only when `GOOGLE_CLIENT_ID` set); persistent remember-me (30d, JDBC). **CSRF is enabled**
(exempt: `/billing/webhook`, `/h2-console/**`). Login throttling via `LoginThrottleFilter`. Everything
not allow-listed in `SecurityConfig` requires authentication. **There is no token/bearer auth.**

## Real-time
`GET /chats/events` SSE (`ChatEventStream`), fed by `NewMailEvent` from polling/IDLE.

## Web ↔ API relationship
Today there is **no REST API**; the only JSON-ish responses are `POST /chats/refresh`
(`{"imported":n}`) and the Stripe webhook. Web controllers call services directly. A mobile API
(`/api/v1/**`) must be a thin layer over the same services — see `docs/API.md` and
`INTERNAL_TODO.md` (logic currently living in `ConversationController`).
