# Backend architecture

Reverse-engineered from the code on 2026-11-11. Where something could not be
confirmed from source it is marked **TBD**.

ConexusMail's backend is a single Spring Boot 3 / Java 21 application. It owns
all persistence and business rules, serves the website (Thymeleaf, see
[WEB.md](WEB.md)), and will serve the mobile client's REST API (see
[API.md](API.md) — **no JSON API exists yet**).

## Stack as implemented

| Concern | What the code uses |
| --- | --- |
| Persistence | **Spring Data JPA / Hibernate** (`ddl-auto: validate`, `open-in-view: false`). Not jOOQ — there is no codegen step. |
| Schema | Flyway, `src/main/resources/db/migration`, V1–V35 |
| Database | PostgreSQL 16 (prod, local); H2 for tests |
| Web | Spring MVC + Thymeleaf, server-rendered |
| Auth | Spring Security: form login, optional Google OIDC, JDBC remember-me |
| Mail | Jakarta Mail (IMAP import, IMAP IDLE), `JavaMailSender` (SMTP out) |
| Billing | `stripe-java` behind gateway interfaces |
| HTML hygiene | jsoup |
| Tests | JUnit 5, Spring Boot Test, spring-security-test, GreenMail, Testcontainers |

## Packages (`com.emailmessenger`)

| Package | Responsibility |
| --- | --- |
| `domain` | JPA entities and enums (`User`, `MailAccount`, `EmailThread`, `Message`, `MessageRecipient`, `Participant`, `Attachment`, `Subscription`, `Plan`, `SavedSearch`, `Team*`, token and audit entities). |
| `repository` | Spring Data repositories. `EmailThreadRepository` holds the conversation-list and timeline queries. |
| `email` | Mailbox integration: `MailAccountService` (connect/delete, plan-limit gate), `MailboxPollingService` + `MailboxPollingScheduler` + `PollingPolicy`, `ImapIdleService`, `EmailImportService` + `MimeMessageParser`, `GmailOAuthClient`, `CredentialEncryptor`, `OwnerAddressService`. Publishes `NewMailEvent`. |
| `service` | Pure-ish conversation logic: `ConversationKeyService`, `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService` (SMTP send), and the view-model records (`ChatConversation`, `ChatMember`, `SenderBubbleRun`, `BubbleMessage`, `ConversationListItem`, `ThreadRef`). |
| `web` | Controllers, form objects, model advice, plus some services that live here: `ChatService`, `ConversationListService`, `OutboundMessageService`, `SavedSearchService`, `OnboardingService`, `ChatEventStream`. |
| `auth` | `SecurityConfig`, registration/login, email verification, password reset, Google OIDC provisioning, login throttling, auth-event audit, account settings. |
| `billing` | Stripe checkout / portal / webhook, `PlanLimits` + `PlanLimitService`, billing banners, trial-end emails. |
| `team` | Teams and invites. |
| `digest`, `reengagement`, `activation` | Scheduled lifecycle emails, each behind an `*.enabled` flag. |
| `admin` | Operator revenue/churn/win-back dashboard and weekly digest, gated by the `admin.emails` allowlist. |

## Data model overview

Tables (one Flyway migration set, V1–V35): `users`, `persistent_logins`,
`subscriptions`, `plan_change_events`, `mail_accounts`, `email_threads`,
`messages`, `message_recipients`, `participants`, `attachments`,
`saved_searches`, `digest_email_preferences`, `password_reset_tokens`,
`email_verifications`, `auth_events`, `teams`, `team_members`, `team_invites`.
(`thread_notes` was created in V25 and dropped in V33.)

- A `User` owns `MailAccount`s (IMAP password or Gmail OAuth, credentials
  encrypted by `CredentialEncryptor`) and `EmailThread`s.
- An `EmailThread` groups `Message`s by `Message-ID` / `In-Reply-To` /
  `References`. Each thread carries a `conversation_key` (V32).
- A `Message` has a sender `Participant`, `MessageRecipient`s (TO/CC/BCC),
  `Attachment` metadata, and an outbound flag (V30). **Attachment bytes are not
  stored** — `blob_ref` is always null.
- `Subscription` holds the user's `Plan` (`FREE`, `PRO`, `BUSINESS`) and Stripe
  state.

## Core business rules

These are the rules a client must mirror. The server remains authoritative.

**Conversation key** (`ConversationKeyService`). A conversation is the set of
people involved, not a thread. The key is the first 32 hex characters of a
SHA-256 over the thread's non-owner participant emails — every message's sender
plus its To/Cc recipients, minus the owner's own addresses — lowercased,
de-duplicated, and sorted. Bcc is excluded. A thread whose only participant is
the owner hashes a fixed sentinel (`" self"`) instead. The hash input is the
sorted emails joined with `\n`, UTF-8 encoded. Clients should still treat keys
as opaque and let the server compute them.

**Conversation list** (`ConversationListService`). Rows are conversations,
most-recent first, 30 per page, with filters `ALL`, `UNREAD`, `AWAITING`,
`ATTACHMENTS`, an optional text query, and an optional mailbox scope. Counts
per filter come from `counts(owner, scope)`.

**Mailbox scope.** Scoping to one connected account only applies when the user
has more than one mailbox; with a single mailbox every view is unscoped.

**Timeline** (`ChatService` → `ConversationService.buildChatConversation`).
All messages sharing a key, quoted replies stripped, grouped into runs of
consecutive same-side messages within a thread. Opening a conversation marks
its threads read.

**Sending.** New mail goes to the conversation's members. The From account is
the scoped mailbox, else the user's default sending account, else the sole
connected mailbox, else the login email. The account's signature is appended
under `"\n\n-- \n"`. "Reply" mode continues the most recently active thread;
otherwise a new email is sent with the given subject or `(no subject)`. Upload
caps are 10 MB per file and 25 MB per request.

**Plan limits** (`PlanLimits`).

| Plan | Mailboxes | Threads | Saved searches |
| --- | --- | --- | --- |
| FREE | 3 | 500 | unlimited |
| PRO | 5 | unlimited | unlimited |
| BUSINESS | unlimited | unlimited | unlimited |

**Polling** (`PollingPolicy`). FREE polls every 15 minutes, paid plans every
5; a user preference can slow this but never go below the plan floor. ±30 s
jitter. Five consecutive failures suspend polling for that account until a
successful manual sync. Paid-tier accounts additionally hold an IMAP IDLE
connection (`mailbox.idle.enabled`).

## Auth model

One `SecurityFilterChain` (`auth/SecurityConfig`), entirely browser-oriented:

- Session cookie (`JSESSIONID`) from form login at `POST /login`
  (`email` / `password`), with `LoginThrottleFilter` ahead of it.
- Optional Google OIDC login, wired only when client id and secret are set.
- Remember-me: persistent JDBC token, 30 days.
- CSRF is enabled for everything except `/billing/webhook` and `/h2-console/**`.
- Public: marketing, auth, legal, PWA assets, static assets, `/billing/webhook`,
  `/digest/opt-out`, `/actuator/health`. Everything else requires a session.
- `/admin/**` additionally requires the caller's email to be in `admin.emails`;
  others get a 404.

There is **no token or bearer authentication**, so nothing a native client can
use today. See [API.md](API.md).

## How the website relates to the REST API

Today the website is the only client and there is no REST API. Controllers
call services directly and return templates or redirects.

The target arrangement is that Thymeleaf controllers and `/api/**` controllers
are both thin adapters over the same services. That is not yet true for chat:
`ConversationController` itself holds send orchestration, sender-account
selection, signature appending, recipient parsing, and mailbox scoping. Those
must move into a service before an API controller is added, or the two paths
will diverge. Tracked in [`INTERNAL_TODO.md`](../INTERNAL_TODO.md).

`GlobalExceptionHandler` renders the HTML `error` template (or redirects) for
every exception, including for requests that expect JSON.

## Live delivery

`MailboxPollingService` publishes `NewMailEvent(userId, importedCount)`.
`ChatEventStream` fans it out after commit to that user's open SSE emitters at
`GET /chats/events`. Details in [API.md](API.md).

## Configuration

Base `application.yml` reads everything from environment variables;
`application-local.yml` targets a local PostgreSQL. Secrets (DB, SMTP, Stripe,
Google OAuth, mailbox encryption password and salt) are never committed.
Schedulers other than mailbox polling and IDLE stay off unless their
`*.enabled` flag is true.
