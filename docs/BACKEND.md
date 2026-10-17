# Backend architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Persistence is **Spring Data JPA (Hibernate) + Flyway**, not jOOQ.
PostgreSQL 16 in dev/prod, H2 in tests. Server-rendered Thymeleaf UI is the only client today; there is **no JSON REST API for the mobile app yet** (see `API.md`, `INTERNAL_TODO.md`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login, Google OIDC, registration, password reset, email verification, login throttle, account settings, auth-event audit |
| `billing` | Stripe checkout/portal/webhook, plans (`FREE/PRO/BUSINESS`), `PlanLimits` caps (mailboxes, threads), trial nudges, banners |
| `email` | Mailbox connect (IMAP password + Gmail OAuth), IMAP polling + IDLE, MIME parsing, import, credential encryption |
| `service` | Conversation domain logic: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records |
| `web` | `/chats` workspace, mailboxes UI, saved searches, landing/pricing/legal, SEO, PWA, SSE stream |
| `team` | Teams + invites. `digest`, `activation`, `reengagement`, `admin` | Schedulers/emails/metrics |
| `domain`, `repository` | JPA entities, Spring Data repos |

## Data model (Flyway V1–V35)
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts` (auth_type, last_seen_uid / sent_uid cursors, signature), `email_threads` (owner, `conversation_key`, unread), `messages` (incl. outbound flag), `message_recipients`, `participants`, `attachments` (`blob_ref` always null: bytes not stored), `saved_searches`, `digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`, `teams`, `team_members`, `team_invites`, `persistent_logins`.
A *conversation* is keyed by `conversationKey` (participant-set hash) and merges one or more threads.

## Auth model
Session cookie (JSESSIONID) + remember-me; CSRF enabled (ignored only for `/billing/webhook`, `/h2-console`). Everything not listed in `SecurityConfig` permitAll requires authentication. Google OIDC when client env vars are present. No token/bearer auth exists yet.

## Web vs API
All controllers are Thymeleaf (`@Controller`) except `BillingWebhookController` (`@RestController`) and `POST /chats/refresh`, `GET /chats/events` (SSE). Logic lives in services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`, ...); a future REST layer must call these, not controllers.

## Live updates
`MailboxPollingService` imports mail and publishes `NewMailEvent`; `ChatEventStream` fans out via SSE; paid tiers get IMAP IDLE (`ImapIdleService`).
