# Backend architecture

Classification: this repo (`email-messenger`) is the BACKEND — `pom.xml`, Spring Boot 3,
Flyway migrations (`V1`…`V35`), Thymeleaf templates. Deviation from the generic target
stack: persistence is **Spring Data JPA** (no jOOQ, no codegen).

## Stack
Java 21, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL 16 (prod/local) / H2 (tests),
Thymeleaf, Spring Security, Stripe, Jakarta Mail (IMAP/SMTP), Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers, form objects, `ChatService`, `ChatEventStream` (SSE), PWA/SEO controllers |
| `service` | Conversation building (`ConversationService`, `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`) |
| `email` | Mailbox integration: IMAP client, IDLE, Gmail OAuth, polling, import, MIME parsing |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (overview)
`User`, `Subscription`, `MailAccount` (IMAP password or Gmail OAuth; credentials encrypted via
`CredentialEncryptor`), `EmailThread`, `Message`, `MessageRecipient`, `Participant`,
`Attachment` (metadata only; `blob_ref` always null), `SavedSearch`, `Team*`, token tables,
`AuthEvent`, `PlanChangeEvent`. Conversations in the UI are keyed by a participant-set
`conversationKey`, not by thread id. Schema is owned by Flyway; migrations are immutable.

## Auth model
Session cookie (`JSESSIONID`) after form login (`POST /login`, params `email`/`password`) or
Google OIDC; remember-me cookie (30 days); CSRF **enabled** (exempt: `/billing/webhook`,
`/h2-console/**`). Everything not on the permit-list in `SecurityConfig` requires auth.
There is **no token/API-key auth** — a mobile client cannot authenticate today (see
`INTERNAL_TODO.md`).

## Web vs. API
The backend is entirely server-rendered. The only JSON-returning endpoint for the app is
`POST /chats/refresh` (plus the Stripe webhook). Business logic lives in services; controllers
resolve the `User` from `Principal` and delegate. Any future `/api/v1/**` controllers must call
the same services (`ChatService`, `ReplyService`, `MailAccountService`, …).
See `docs/API.md` and `docs/WEB.md`.
