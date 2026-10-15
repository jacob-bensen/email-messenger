# BACKEND.md — email-messenger (ConexusMail)

Classification: BACKEND (pom.xml, Spring annotations, Flyway migrations V1–V35). Sibling: `conexus-mail-mobile` (FRONTEND, Flutter; currently only a README).

## Stack (as implemented)
Java 21, Spring Boot 3, Spring Data **JPA** (NOT jOOQ — no jOOQ codegen exists), PostgreSQL 16 (H2 in tests), Flyway, Thymeleaf, Spring Security (form login + Google OIDC), Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, legal, SEO, PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, bubble/run view records |
| `email` | IMAP/SMTP: `EmailImportService`, `MimeMessageParser`, `MailboxPollingService` (+scheduler), `ImapIdleService`, `MailAccountService`, Gmail OAuth client, `CredentialEncryptor` |
| `auth` | `SecurityConfig`, registration/login/password reset/email verify, Google OIDC, account pages, login throttle, auth audit events |
| `billing` | Stripe checkout/portal/webhook, `PlanLimitService`, plans FREE/PRO/BUSINESS, trial nudges |
| `team` | Team invites/membership |
| `digest`, `activation`, `reengagement` | Scheduled emails |
| `admin` | `/admin/revenue` operator metrics |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema overview (Flyway, immutable once merged; next free number: V36)
users, subscriptions (+billing period, cancellation reason, win-back/trial tracking), mail_accounts (auth type IMAP|OAuth, polling policy, last-seen UIDs, signature), email_threads (owner, `conversation_key`, unread), messages (+outbound flag), message_recipients, participants, attachments (`blob_ref` always null today), saved_searches, digest_email_preferences, password_reset_tokens, email_verifications, auth_events, teams/team_members/team_invites, plan_change_events, persistent remember-me tokens.

## Auth model
- Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,`password`) or Google OIDC (`/auth/google/start`); remember-me cookie 30 days.
- CSRF is ON for everything except `/billing/webhook` and `/h2-console/**`.
- Everything not on the permitAll list in `SecurityConfig` requires an authenticated session. There is **no token/bearer auth** → a mobile client cannot authenticate today (see INTERNAL_TODO P0).
- Login throttling (`LoginThrottleFilter`), email verification, plan limits enforced server-side.

## Web pages vs REST API
There is currently **no REST API**. All business logic sits in services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`, `BillingService`, …) called by Thymeleaf controllers. Controllers still contain some orchestration (mailbox selection, scope address, filter parsing in `ConversationController`) that a JSON controller would need to reuse → extract into a service before adding the API (INTERNAL_TODO).
Only JSON/non-HTML endpoints: `POST /chats/refresh`, `GET /chats/events` (SSE), `POST /billing/webhook`, PWA/SEO resources. See `API.md`.

## Background work
`@Scheduled` mailbox polling (`mailbox.*` flags), IMAP IDLE for paid plans (`mailbox.idle.enabled`), digest/activation/reengagement/trial-end schedulers.
