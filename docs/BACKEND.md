# Backend architecture

Repo classification: **BACKEND** (evidence: `pom.xml`, Spring Boot 3 / Java 21, Flyway migrations V1–V35, Thymeleaf templates).
Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate, not jOOQ** (no codegen step). Web and (future) API share services in `service/`, `web/*Service`, etc.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, login throttle, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial/cancel flows |
| `email` | IMAP/Gmail-OAuth mailbox connect, polling (`MailboxPollingService`), IMAP IDLE (paid tiers), RFC 822 parsing (`MimeMessageParser`), import (`EmailImportService`) |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation view models, `ReplyService`, `ConversationKeyService` |
| `web` | Thymeleaf controllers, `ConversationListService`, `ChatService`, `OutboundMessageService`, SSE (`ChatEventStream`), PWA shell |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin revenue/retention metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (Flyway, `src/main/resources/db/migration`)
Tables: `users`, `persistent_logins`, `subscriptions`, `plan_change_events`, `mail_accounts`, `participants`, `email_threads` (has `conversation_key`, owner, unread), `messages` (outbound flag), `message_recipients`, `attachments` (`blob_ref` always null today), `saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `teams`, `team_members`, `team_invites`. (`thread_notes` dropped in V33.)
A *conversation* (`/chats/{key}`) is keyed by the participant-set `conversationKey`, merging several `email_threads`.

## Auth model
- Session cookie (`JSESSIONID`) + optional remember-me (30 days, persistent token repo).
- **CSRF enabled** for everything except `/billing/webhook` (Stripe signature) and `/h2-console/**`.
- Public: marketing, legal, auth pages, PWA assets, `/actuator/health`. Everything else requires login. `/admin/**` additionally checked by `AdminAuthorizer`.
- No bearer-token / API-key auth exists. **A mobile client cannot authenticate today** (see `INTERNAL_TODO.md`).

## Web vs REST
All user-facing behavior is server-rendered HTML. The only JSON endpoint is `POST /chats/refresh`; `/chats/events` is an SSE stream. There is no `/api/**` namespace. See `API.md` and `WEB.md`.
