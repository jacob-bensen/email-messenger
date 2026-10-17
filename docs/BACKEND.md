# Backend architecture

Spring Boot 3.5 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf website plus
background jobs. **There is no JSON REST API for a mobile client yet** (see `docs/API.md`).

## Stack (as implemented)
- Spring MVC + Thymeleaf, Spring Security (form login, Google OIDC, remember-me, CSRF on).
- **Spring Data JPA** (not jOOQ), Flyway (`V1`..`V35`), PostgreSQL 16 / H2 in tests.
- Jakarta Mail (IMAP/SMTP), Stripe Java SDK, jsoup.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | registration, login, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP client, IDLE, polling, MIME parsing, import, Gmail OAuth, credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, conversationKey |
| `web` | MVC controllers, chat SSE (`ChatEventStream`), mailbox/saved-search/PWA/SEO/legal |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | teams+invites, digests, admin metrics, lifecycle emails |

## Data model (overview)
`User`, `Subscription`/`Plan`, `MailAccount` (IMAP password or Gmail OAuth, encrypted creds, UID cursors),
`EmailThread` (owner, `conversationKey` = participant set), `Message` (+`MessageRecipient`, `Attachment`,
outbound flag), `Participant`, `SavedSearch`, `Team`/`TeamMember`/`TeamInvite`, `AuthEvent`, `PlanChangeEvent`,
`DigestEmailPreference`, reset/verification tokens. Schema source of truth: `src/main/resources/db/migration`.

## Auth model
Session cookie (`JSESSIONID`) + CSRF token for all POSTs except `/billing/webhook`. Everything not
listed as public in `SecurityConfig` requires authentication. No token/bearer auth exists.

## Web vs. API
Controllers call shared services (`ConversationService`, `ReplyService`, `OutboundMessageService`,
`MailboxPollingService`). Only JSON-ish endpoints today: `POST /chats/refresh` and the SSE `/chats/events`.
A future mobile API must reuse these services, not the controllers.
