# Backend architecture (ConexusMail)

Reverse-engineered 2026-10-03 from the code. Unknowns are marked TBD.

## Stack
Java 21, Spring Boot 3, Spring MVC + Thymeleaf, Spring Data **JPA/Hibernate**
(`ddl-auto: validate`, `open-in-view: false`), Flyway (V1..V35), PostgreSQL 16
(prod/local), H2 (tests), Maven wrapper. **jOOQ is not used** (see INTERNAL_TODO).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, landing/pricing/legal/PWA/SEO), SSE (`ChatEventStream`), list/chat services |
| `auth` | Form login, Google OIDC, registration, password reset, email verify, account pages, login throttle, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP/SMTP import (`EmailImportService`, `MimeMessageParser`), polling + IDLE, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, conversation keys, view-model records |
| `team` | Teams and invites |
| `digest`, `activation`, `reengagement` | Scheduled emails |
| `admin` | `/admin/revenue` metrics (admin-only via `AdminAuthorizer`) |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (overview)
`User`, `Subscription`/`Plan`, `MailAccount` (per-mailbox credentials, `AuthType`
password|oauth, last-seen UID cursors), `EmailThread` (owner, `conversationKey`
= participant-set key), `Message` (+ `MessageRecipient`, `Attachment` — `blob_ref`
always null today), `Participant`, `SavedSearch`, `Team*`, `PasswordResetToken`,
`EmailVerificationToken`, `AuthEvent`, `PlanChangeEvent`, `DigestEmailPreference`.
Authoritative schema: `src/main/resources/db/migration`.

## Auth model
Session cookie (JSESSIONID) + remember-me (30 d, persistent token repo). CSRF
**enabled** (exempt: `/h2-console/**`, `/billing/webhook`). Form login at `POST /login`
(`email`, `password`); Google OIDC when `GOOGLE_CLIENT_ID/SECRET` set. All routes
not listed in `SecurityConfig` permitAll require authentication. There is **no
token/API-key auth** — nothing a mobile client can use today.

## Web vs REST
The app is server-rendered only. The sole JSON endpoints are `POST /chats/refresh`
and `POST /billing/webhook`. No REST API for the mobile client exists yet; see
`docs/API.md` for the current surface and the planned `/api/v1` contract status.
Controllers delegate to services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, …); any future REST controllers
must reuse these.

## Background work
`MailboxPollingScheduler` (polling), `ImapIdleService` (paid tiers, flag
`mailbox.idle.enabled`), digest/activation/reengagement/trial schedulers.
