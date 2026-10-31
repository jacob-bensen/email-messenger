# BACKEND — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail", repo `email-messenger`). It serves
the public website and the authenticated web app via Thymeleaf, owns persistence and
all business rules. **There is no REST API for the mobile client yet** — see
`docs/API.md` (contract is currently a *proposal*) and `INTERNAL_TODO.md`.

Stack as implemented (differs from the generic routine assumption): Spring Data
**JPA/Hibernate** (not jOOQ — no codegen step), PostgreSQL 16 (H2 in tests), Flyway
(`src/main/resources/db/migration`, V1–V35), Maven wrapper, Spring Security.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = `/chats` workspace, `MailboxController`, `GmailConnectController`, `SavedSearchController`, `PricingController`, `PwaController`, `SeoController`, `ChatEventsController` SSE), view-prep services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`), `GlobalExceptionHandler` |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner` (quote/signature stripping), `ConversationKeyService` (participant-set key), `ReplyService` (SMTP send), view records `ChatConversation`, `ConversationListItem`, `BubbleRun`, `BubbleMessage` |
| `email` | IMAP/Gmail integration: `EmailImportService`, `MimeMessageParser`, `MailboxPollingService/Scheduler`, `ImapIdleService` (paid tiers), `GmailOAuthClient`, `CredentialEncryptor`, `MailAccountService` |
| `auth` | Form login + Google OIDC, registration, email verification, password reset, login throttle, account pages, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial conversion, banners |
| `team` | Teams and invites |
| `digest`, `reengagement`, `activation` | Scheduled emails and nudges |
| `admin` | `/admin/revenue` metrics, win-back outreach |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (overview)
`users`, `subscriptions` (+ `plan_change_events`), `mail_accounts` (IMAP password or
Gmail OAuth; encrypted credentials, polling cursors `last_seen_uid`,
`last_seen_sent_uid`, signature), `email_thread` (owner, unread, `conversation_key`),
`message` (+ `message_recipient`, `participant`, `attachment` — `blob_ref` always
null, bytes are not stored), `saved_searches`, `teams/team_members/team_invites`,
`digest_email_preferences`, `auth_events`, token tables, remember-me tokens.
A *conversation* in the UI = all threads sharing a `conversationKey` (hash of the
participant set), scoped optionally to one connected mailbox.

## Auth model
Session cookie (`JSESSIONID`) + remember-me (30 d, persistent token repo), CSRF
enabled (only `/billing/webhook` and `/h2-console/**` exempt). Form login posts
`email`/`password` to `/login`. Google OIDC only when `GOOGLE_CLIENT_ID` is set.
Everything not in the permit-list in `SecurityConfig` requires authentication.
**No token/bearer auth exists** — a prerequisite for the mobile client.

## Web vs API
All current endpoints are web (HTML, form posts, redirects with flash attributes).
Exceptions returning data: `POST /chats/refresh` (JSON `{imported:n}`) and
`GET /chats/events` (SSE). Business logic is partly inside `ConversationController.send`
(recipient resolution, signature append, reply-vs-new branching) rather than a
service — it must be extracted before a REST controller can share it (see TODO).
