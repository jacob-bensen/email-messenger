# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf
website plus background mail jobs. **There is currently no JSON REST API** —
see `docs/API.md`. Sibling repo: `conexus-mail-mobile` (Flutter client, not
yet implemented).

> Stack note: the generic sync routine assumes jOOQ; this repo uses
> **Spring Data JPA** (Hibernate, `ddl-auto: validate`) + Flyway. There is no
> jOOQ codegen step. Follow the existing JPA conventions.

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `domain` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail import: `MimeMessageParser`, `EmailImportService`, `MailboxPollingService` (+scheduler), `ImapIdleService`, `GmailOAuthClient`, `CredentialEncryptor` |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService`, `ReplyService`, view-model records |
| `web` | Thymeleaf controllers + SSE (`ChatEventStream`), `OutboundMessageService`, `ChatService`, model advices |
| `auth` | Spring Security config, form + Google OIDC login, registration, password reset, email verification, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digests, admin metrics, onboarding & win-back |

## Data

PostgreSQL 16 (prod/local), H2 in tests. Flyway migrations in
`src/main/resources/db/migration` (`V1`..`V35` at time of writing; immutable
once merged). Core model: `User` → `MailAccount`(s) → `EmailThread` →
`Message` (+ `MessageRecipient`, `Attachment`), `Participant` deduped by
normalized address. Chats are keyed by participant-set `conversationKey`
(`ConversationKeyService`). Attachments: `blob_ref` is always null (bytes not
stored).

## Auth model

Spring Security, **session cookie + CSRF** (CSRF ignored only for
`/billing/webhook` and `/h2-console/**`). Form login (`/login`, params
`email`/`password`), remember-me (JDBC token repo), optional Google OIDC.
Everything not in the `permitAll` list in `auth/SecurityConfig` requires
authentication. **No token (Bearer/JWT) auth exists** — a mobile client cannot
authenticate yet (TBD design: see INTERNAL_TODO).

## Web vs. API

All controllers are `@Controller` returning Thymeleaf views, except:
- `POST /chats/refresh` → `{"imported": n}` (JSON, session-auth, used by chat JS)
- `GET /chats/events` → SSE stream (`ChatEventStream`)
- `POST /billing/webhook` (Stripe)
Business logic lives in services (`ChatService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`, ...). A future REST layer must
call the same services.

## Commands

See `CLAUDE.md`. Build/test: `./mvnw test` (or `mvn -q -B verify`).
