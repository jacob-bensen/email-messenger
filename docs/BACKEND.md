# BACKEND — ConexusMail server architecture

Spring Boot 3 / Java 21 monolith (`com.emailmessenger`). It renders the website
with Thymeleaf; there is **no REST API for the mobile client yet** (see
`API.md`). Persistence is Spring Data JPA (not jOOQ) over PostgreSQL 16 (H2 in
tests), schema owned by Flyway (`src/main/resources/db/migration`, V1..V35,
immutable once merged).

## Packages
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, Gmail OAuth connect, saved searches, PWA, SEO, legal, pricing |
| `auth` | Form login + Google OIDC, registration, email verification, password reset, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), billing banners |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling + IDLE, Gmail OAuth client, credential encryption |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digests, lifecycle emails, operator metrics |

## Data model (overview)
`User` → `MailAccount` (IMAP password or Gmail OAuth; polling policy, last-seen
UIDs) → `EmailThread` (owner, `conversation_key` = participant set) →
`Message` (+ `MessageRecipient`, `Attachment`, `outbound` flag) →
`Participant` (deduped by normalized address). Billing: `Subscription`,
`PlanChangeEvent`. Other: `SavedSearch`, `Team`/`TeamMember`/`TeamInvite`,
`AuthEvent`, `EmailVerificationToken`, `PasswordResetToken`.

## Auth model
Session cookie (form login or Google OIDC), CSRF enabled (ignored only for
`/h2-console/**` and `/billing/webhook`). Everything except public pages is
`authenticated()`. **No token/bearer auth exists** — a mobile client cannot
authenticate today (see INTERNAL_TODO).

## Web vs API
All business rules live in `service`/domain services; controllers call them
through `Principal` → `UserService.requireByEmail`. The only JSON endpoints are
`POST /chats/refresh`, the SSE stream `GET /chats/events`, and the Stripe
webhook. Any future `/api/v1` controller MUST reuse these services.
