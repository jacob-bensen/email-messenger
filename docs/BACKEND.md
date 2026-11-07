# Backend architecture

Classification: BACKEND (pom.xml, Spring Boot 3.5, Flyway migrations V1–V35,
JPA entities, Thymeleaf templates). Reverse-engineered 2026-11-07.

> Deviation from the generic target stack: persistence is **Spring Data JPA /
> Hibernate**, not jOOQ. There is no jOOQ codegen. Do not introduce jOOQ;
> match the JPA style.

## Stack
Java 21, Spring Boot 3.5.14, Spring Security (form login + Google OIDC +
remember-me), Spring Data JPA, Flyway, Thymeleaf, Jakarta Mail (IMAP/SMTP),
Stripe SDK. PostgreSQL 16 in prod/local, H2 in tests.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`ConversationController` = /chats workspace), SSE (`ChatEventsController`/`ChatEventStream`), PWA/SEO/legal/pricing pages, list/chat/onboarding services |
| `service` | Chat domain logic: `ChatService`, `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`; view records (`ChatConversation`, `BubbleRun`, `BubbleMessage`, `ConversationListItem`) |
| `email` | Mailbox integration: IMAP client, IDLE (`ImapIdleService`), Gmail OAuth, `MailboxPollingService`/Scheduler, `EmailImportService`, `MimeMessageParser`, credential encryption |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, tokens, events) |
| `repository` | Spring Data repositories |
| `auth` | Security config, registration/login/reset/verify, Google OIDC, account settings, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`, `PlanLimitService`), banners, trial conversion |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin revenue/retention metrics |

## Data model (Flyway, `src/main/resources/db/migration`, V1–V35)
Tables: users, mail_accounts, email_threads, messages, message_recipients,
participants, attachments (`blob_ref` always null — bytes not stored),
subscriptions, plan_change_events, teams, team_members, team_invites,
saved_searches, thread_notes, digest_email_preferences, auth_events,
password_reset_tokens, email_verifications, persistent_logins (remember-me).
Conversations are **not** a table: a chat is identified by a
`conversationKey` derived from the participant set (`ConversationKeyService`)
and merges multiple `email_threads`.
Plans: FREE / PRO / BUSINESS (`domain/Plan`), limits in `billing/PlanLimits`.

## Auth model
- Browser session cookie (`JSESSIONID`) + persistent remember-me (30 days,
  JDBC token store). CSRF **enabled** except `/h2-console/**` and
  `/billing/webhook`.
- Login: `POST /login` form (`email`, `password`); Google OIDC when
  `GOOGLE_CLIENT_ID` set. Throttled by `LoginThrottleFilter`.
- Everything not in the permitAll list (see `SecurityConfig`) requires
  authentication; controllers resolve the user with
  `userService.requireByEmail(principal.getName())`.
- **No token / API-key auth exists.** A mobile client cannot authenticate
  today (see `docs/API.md`, `INTERNAL_TODO.md`).

## Web vs REST
The app is entirely server-rendered. There is no `/api/**` and no
`@RestController` except the Stripe webhook. Mobile REST work must reuse the
service layer (`ChatService`, `ConversationListService`, `ReplyService`,
`OutboundMessageService`, `MailAccountService`, `PlanLimitService`) and not copy
controller logic. Known controller-resident logic to extract first: see
`INTERNAL_TODO.md` ("send orchestration in ConversationController").

## Background work
`@Scheduled` pollers (`MailboxPollingScheduler`), IMAP IDLE for paid plans
(`mailbox.idle.enabled`), digest/lifecycle schedulers. A poll that imports mail
publishes `NewMailEvent(userId, importedCount)` → `ChatEventStream` (SSE).
