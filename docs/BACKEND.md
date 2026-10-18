# Backend architecture

Reverse-engineered from the code on 2026-10-18. Anything not verifiable is marked **TBD**.

## Stack (actual)
Java 21, Spring Boot 3, Spring MVC + Thymeleaf, Spring Security, **Spring Data JPA / Hibernate**
(`ddl-auto: validate`, `open-in-view: false`), PostgreSQL (prod/local) / H2 (tests), Flyway, Maven wrapper.
There is **no jOOQ** and no codegen step. Stripe for billing, Jakarta Mail for IMAP/SMTP, Gmail OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, pricing/landing, legal, PWA, SEO, SSE) and `@ControllerAdvice` |
| `auth` | Security config, registration/login, Google OIDC, password reset, email verification, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial conversion |
| `email` | IMAP/Gmail import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService/Scheduler`), IMAP IDLE, credential encryption |
| `service` | Conversation/IM transform (`IMTransformService`, `EmailBodyCleaner`), `ChatConversation`, `ConversationKeyService`, `ReplyService` (SMTP send) |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, weekly digests, lifecycle emails, admin revenue/retention metrics |

(Some chat services such as `ChatService`, `ConversationListService`, `OutboundMessageService` live outside the paths listed above in
`web`/`service`; see source.)

## Schema overview
Flyway migrations `V1`–`V35` in `src/main/resources/db/migration` (immutable once merged). Core tables: users, subscriptions,
mail_accounts (encrypted credentials, `last_seen_uid`, `last_seen_sent_uid`, `auth_type`), email_thread (owner, unread,
`conversation_key`), message (outbound flag), message_recipient, participant, attachment (`blob_ref` always null today),
saved_search, teams/invites/members, auth_events, password/email tokens, plan_change_events, persistent remember-me tokens.

## Auth model
Session cookie (`JSESSIONID`) via form login (`email`/`password` at `POST /login`) or Google OIDC (only when
`GOOGLE_CLIENT_ID` is set). Remember-me cookie, 30 days, JDBC token store. **CSRF is enabled** for everything except
`/h2-console/**` and `/billing/webhook`. Everything not on the public allow-list in `SecurityConfig` requires authentication;
unauthenticated requests redirect to `/login` (HTML), there is no 401 JSON behavior. **No token / API-key auth exists.**

## Web vs REST
All user-facing functionality is server-rendered Thymeleaf. There is **no `@RestController` API for a mobile client**.
Only three JSON/stream-ish endpoints exist (see `API.md`). Controllers delegate to services, which is the layer a future
REST API must reuse.
