# BACKEND.md — architecture

Spring Boot 3 / Java 21 monolith (`com.emailmessenger`) that serves the website
(Thymeleaf), handles billing, and imports mail. Persistence is **Spring Data JPA**
(not jOOQ) on PostgreSQL 16 (H2 in tests); schema is managed by Flyway
(`src/main/resources/db/migration`, V1..V34, immutable once merged).

## Packages
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/legal/PWA/SEO), SSE stream `/chats/events`, `ChatService`, `OutboundMessageService` |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService` |
| `email` | Mailbox integration: IMAP polling/IDLE, Gmail OAuth, `EmailImportService`, `MimeMessageParser`, `CredentialEncryptor` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account pages, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, admin metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (overview)
`User` → `MailAccount`(s) (IMAP password or Gmail OAuth; last-seen UID cursors) →
`EmailThread` (owner, `conversationKey` = participant set, unread) → `Message`
(+`MessageRecipient`, `Attachment`; `outbound` flag). `Participant` deduped by
normalized address. Also `Subscription`/`PlanChangeEvent`, `SavedSearch`, `Team`/
`TeamMember`/`TeamInvite`, token tables, `AuthEvent`.

## Auth model
Session-cookie (JSESSIONID) form login at `/login`, plus optional Google OIDC.
CSRF is enabled (ignored only for `/billing/webhook`). All routes except the public
allow-list in `auth/SecurityConfig.java` require authentication.
**There is no token/API-key auth today** — a mobile client cannot authenticate
non-interactively (see INTERNAL_TODO).

## Web vs REST
Everything is server-rendered HTML; the only JSON-ish endpoint is the Stripe
webhook (`BillingWebhookController`) and an SSE stream. There is **no REST API**
yet. Controllers delegate to services (`ChatService`, `ReplyService`,
`MailAccountService`, `PlanLimitService`, …); a future `/api/v1` layer must reuse
those services rather than the controllers.

## Commands
See `CLAUDE.md`.
