# BACKEND.md — architecture

Repo classification: **BACKEND** (evidence: `pom.xml`, Spring Boot 3 / Java 21, Flyway migrations in
`src/main/resources/db/migration`, Thymeleaf templates). Sibling repo: `conexus-mail-mobile` (Flutter, FRONTEND).

> Deviation from the generic stack assumption: persistence is **Spring Data JPA/Hibernate**, not jOOQ.
> There is no jOOQ codegen step. Schema is owned by Flyway (V1..V35 at time of writing).

## Product
ConexusMail turns email into a texting-style conversation view. A *conversation* (`conversationKey`) is the
set of participants; it merges every email thread with that participant set into one timeline.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web/` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/pricing`, PWA, SEO, legal), list/chat services, SSE stream |
| `service/` | IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationService`, `ReplyService`, `ConversationKeyService`, view records (`ChatConversation`, `ConversationListItem`, bubbles) |
| `domain/` | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Participant`, `Attachment`, `Subscription`, `Team*`, `SavedSearch`, ...) |
| `repository/` | Spring Data repositories |
| `email/` | IMAP import/poll/IDLE, Gmail OAuth, MIME parsing, credential encryption |
| `auth/` | Spring Security config, form login, Google OIDC, password reset, email verification, login throttle, account pages |
| `billing/` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial-end nudges |
| `team/`, `digest/`, `activation/`, `reengagement/`, `admin/` | teams + invites, weekly digest, lifecycle emails, operator revenue/churn metrics |

## Data model (overview)
`users` 1—N `mail_accounts` (IMAP password or Gmail OAuth; encrypted credential, per-account polling cursors
`last_seen_uid`, `last_seen_sent_uid`); `users` 1—N `email_threads` (owner-scoped, carries `conversation_key`,
`unread`); `email_threads` 1—N `messages` (`message_id_header`, `in_reply_to`, `outbound`, plain+html body);
`messages` N—1 `participants` (dedup by normalized address), `message_recipients`, `attachments`
(`blob_ref` always null: bytes are not stored). Also `subscriptions`, `plan_change_events`, `teams`,
`team_members`, `team_invites`, `saved_searches`, `digest_email_preferences`, `auth_events`,
`password_reset_tokens`, `email_verifications`, `persistent_logins`.

## Auth model
Session-based Spring Security: form login (`/login`, params `email`/`password`), Google OIDC (only when
`GOOGLE_CLIENT_ID` is set), remember-me (30 days, persistent token table), CSRF **enabled** (exempt:
`/h2-console/**`, `/billing/webhook`). Everything not in the `permitAll` list in `auth/SecurityConfig.java`
requires authentication. `LoginThrottleFilter` rate-limits login by email. **There is no token/bearer auth**
— see INTERNAL_TODO (mobile client blocker).

## Web vs. API
The web app is server-rendered. Controllers resolve the user via `UserService.requireByEmail(principal)` and
delegate to services (`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`,
`MailboxPollingService`). **No REST/JSON API for the mobile client exists yet**; the only JSON/streaming
endpoints are `POST /chats/refresh` (JSON), `GET /chats/events` (SSE), and `POST /billing/webhook`.
When the API is added it must call the same services, not controllers.
