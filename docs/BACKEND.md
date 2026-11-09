# Backend architecture

Classification (Phase 0 audit, 2026-11-09): **BACKEND** = this repo (`pom.xml`, Spring annotations, Flyway migrations V1–V35). Sibling **FRONTEND** = `conexus-mail-mobile` (Flutter; currently only a README — see its `docs/`).

## Reality vs. the generic routine assumptions
- Persistence is **Spring Data JPA/Hibernate**, not jOOQ. There is no jOOQ codegen.
- There is **no REST/JSON API yet**. Everything is server-rendered Thymeleaf + form posts, session-cookie auth with CSRF. The only JSON-ish endpoints are `POST /chats/refresh` (returns `{"imported":n}`) and the SSE stream `GET /chats/events`.
- A mobile client therefore has nothing to call until an API is designed; see `INTERNAL_TODO.md` and `docs/API.md` (proposed contract, marked TBD).

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, Gmail connect, saved searches, SEO/PWA, legal, pricing/landing, demo |
| `service` | Conversation building: `ConversationService`, `ConversationKeyService` (participant-set key), `IMTransformService` + `EmailBodyCleaner` (quote stripping, bubbles), `ReplyService` (outbound) |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`), IDLE (`ImapIdleService`), polling (`MailboxPollingService/Scheduler`), `MimeMessageParser`, `EmailImportService`, Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | `SecurityConfig`, registration, login throttling, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, weekly digest, admin revenue, lifecycle nudges |

## Data model (Flyway `src/main/resources/db/migration`, V1–V35)
Core: `User`, `MailAccount` (IMAP password or Gmail OAuth, encrypted creds), `EmailThread` (has `conversation_key`), `Message`, `MessageRecipient`, `Participant`, `Attachment` (`blob_ref` always null today), `SavedSearch`. Billing: `Subscription`, `PlanChangeEvent`. Teams: `Team`, `TeamMember`, `TeamInvite`. Auth: `AuthEvent`, `EmailVerificationToken`, `PasswordResetToken`, remember-me tokens. Migrations are immutable; add `V36+`.

## Auth model
Form login (`/login`, params `email`/`password`), remember-me (30d, DB-backed), optional Google OIDC (only when `GOOGLE_CLIENT_ID` set). CSRF on for everything except `/h2-console/**` and `/billing/webhook` (Stripe signature). Everything not whitelisted in `SecurityConfig` requires authentication. No token/bearer auth exists.

## Web vs. API
Controllers call services directly (`ConversationService`, `ReplyService`, `MailAccountService`…). Any future `/api/v1` controllers must reuse these services and the `Conversation*` records rather than re-implementing logic.

## Build/test
`./mvnw clean package`, `./mvnw test` (H2 for tests; Postgres 16 locally/prod).
