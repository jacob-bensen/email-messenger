# BACKEND.md — architecture (reverse-engineered 2026-11-07)

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf UI;
**no REST/JSON API exists yet** (see `API.md`). Persistence is Spring Data **JPA**
(not jOOQ) on PostgreSQL 16 (H2 in tests), schema managed by Flyway
(`src/main/resources/db/migration`, V1..V35, immutable once merged).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, password reset, email verification, account settings, login throttling, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `domain` | JPA entities: `User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ... |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`), IDLE, Gmail OAuth, polling (`MailboxPollingService`), import (`EmailImportService`), MIME parsing |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, chat view records |
| `web` | Controllers + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, `ChatEventStream` SSE) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |

## Auth model
Session cookie (form login or Google OIDC), CSRF enabled (ignored only for
`/billing/webhook`, `/h2-console/**`). Public routes are the allow-list in
`SecurityConfig`; everything else requires authentication. No token/bearer
auth exists — a mobile client would need one (TBD, see `INTERNAL_TODO.md`).

## Conversation model
Conversations are keyed by the participant set (`conversationKey`), merging
multiple `EmailThread`s. Mutations: `ReplyService`/`OutboundMessageService`
(send via SMTP), `ChatService.markRead`.

## Web vs API
All behavior is reachable only through Thymeleaf controllers. Services are
shared-by-construction (controllers call services), but because there is no
API there is nothing yet to diverge from.
