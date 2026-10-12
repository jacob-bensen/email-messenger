# Backend architecture

Repo classification: BACKEND (pom.xml, Spring annotations, Flyway migrations under
`src/main/resources/db/migration`, Thymeleaf templates). Sibling: `conexus-mail-mobile`.

Reality check vs. the generic target stack: persistence is **Spring Data JPA** (not jOOQ),
Flyway + PostgreSQL 16 (H2 in tests), Java 21, Maven wrapper. There is **no REST API yet**
(see `docs/API.md`). The only JSON-returning route is `POST /chats/refresh`.

## Package layout (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Spring Security config (form login + Google OIDC, remember-me, CSRF on), registration, password reset, email verification, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial/billing banners |
| `email` | IMAP/Gmail-OAuth mailbox integration, MIME parsing, import (`EmailImportService`), polling, IMAP IDLE, credential encryption |
| `service` | Conversation logic: `IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService`, `ReplyService`; view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`) |
| `web` | Thymeleaf controllers + services for the UI (`ConversationController`, `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream`), PWA/SEO controllers |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, weekly digests, admin metrics, lifecycle schedulers |
| `domain`, `repository` | JPA entities and Spring Data repositories |

## Data model (tables, from Flyway V1..V35)
users, subscriptions, plan_change_events, mail_accounts, email_threads, messages,
message_recipients, participants, attachments (`blob_ref` always null: bytes not stored),
thread_notes, saved_searches, teams, team_members, team_invites, digest_email_preferences,
password_reset_tokens, email_verifications, auth_events, persistent_logins.
Conversations are keyed by `conversationKey` (participant set), merged across threads.

## Auth model
Session cookie (`JSESSIONID`) via form login (`/login`, fields `email`/`password`) or Google
OIDC; remember-me cookie (30 days). CSRF enabled everywhere except `/billing/webhook` and
`/h2-console/**`. Everything not in the permit-all list is authenticated. Mobile-suitable
token auth: **TBD / not implemented**.

## Web vs. API
Controllers call services directly; there is no API layer. The future `/api/v1` controllers
must reuse `ChatService`, `ConversationListService`, `OutboundMessageService`, and
`MailAccountService`, which today are reached only through `web` controllers. Some send/list
orchestration still lives in `ConversationController` (account scoping `selectMailbox`,
`scopeAddress`, `sendingAccount`, signature append) and must move into services first.
