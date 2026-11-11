# Backend architecture

`email-messenger` is the ConexusMail server: a single Spring Boot 3 / Java 21
application that owns all persistence and business rules and serves two
front ends from one process:

1. the **website** — server-rendered Thymeleaf pages (see `docs/WEB.md`), and
2. the **REST API** under `/api/v1` for the mobile app
   (`conexus-mail-mobile`; contract in `docs/API.md`).

> Persistence is **Spring Data JPA (Hibernate)**, not jOOQ. There is no code
> generation step; entities in `domain/` are validated against the Flyway
> schema at startup (`ddl-auto: validate`).

## Packages (`com.emailmessenger`)

| Package | Role |
|---|---|
| `domain` | JPA entities and enums. No Spring dependencies. |
| `repository` | Spring Data repositories. The heavy read queries for the chats list live in `EmailThreadRepository`. |
| `service` | Mail → chat transformation: `ConversationKeyService` (participant-set key), `ConversationService` / `IMTransformService` / `EmailBodyCleaner` (quote stripping, signature lifting, bubble runs), `ReplyService` (SMTP send). View-model records (`ChatConversation`, `ConversationListItem`, …). |
| `email` | Mailbox integration: IMAP clients, `MailAccountService` (connect/list/delete), `EmailImportService`, `MailboxPollingService` + scheduler, `ImapIdleService`, Gmail OAuth, credential encryption. |
| `auth` | Users, registration, form + Google OIDC login, password reset, email verification, login throttle + audit log, `SecurityConfig`. Also API bearer tokens (`ApiTokenService`, `ApiLoginService`, `ApiTokenAuthenticationFilter`). |
| `billing` | Stripe checkout/portal/webhooks, `PlanLimitService` (effective plan + caps), banners and nudges. |
| `web` | Thymeleaf controllers and the read services behind them (`ConversationListService`, `ChatService`, `SavedSearchService`), SSE (`ChatEventStream`), PWA and SEO endpoints, `GlobalExceptionHandler` (HTML errors). |
| `api` | REST controllers, DTO records, `ApiExceptionHandler` (JSON errors). Thin: no business rules. |
| `admin`, `activation`, `reengagement`, `digest`, `team` | Operator metrics page, lifecycle email schedulers, weekly digest, team invites. |

## How the website and the API relate

Both are adapters over the same services; neither may hold rules the other
needs.

```
Thymeleaf controller (web/…Controller) ─┐
                                        ├─► service layer ─► repositories ─► PostgreSQL
REST controller (api/…ApiController) ───┘
```

What is shared today for the one feature both expose (the chats list):

| Concern | Shared code |
|---|---|
| Listing, search, filters, previews, counts | `web.ConversationListService` |
| Which mailbox a request is scoped to | `web.MailboxScope` |
| Parsing the `filter` value | `ConversationListService.ChatFilter.parse` |
| Background refresh of due mailboxes on open | `email.MailboxPollingService.refreshDueForUserAsync` |
| Resolving the signed-in user | `auth.UserService.requireByEmail(principal.getName())` |
| Login throttle + audit rows | `auth.LoginThrottleService`, `auth.AuthEventService` |

**Known violation, to fix before the API grows:** the send/reply/compose flow
(choice of sending account, signature appending, recipient parsing,
reply-vs-new branching) is implemented as private methods inside
`web.ConversationController`. It must move into a service before a send
endpoint is added. See `INTERNAL_TODO.md`.

## Authentication model

Two `SecurityFilterChain`s in `auth.SecurityConfig`:

| | Website chain (`@Order(2)`, everything else) | API chain (`@Order(1)`, `/api/**`) |
|---|---|---|
| Credential | Session cookie (`JSESSIONID`), optional 30-day remember-me cookie, Google OIDC | `Authorization: Bearer cmx_…` |
| State | Server session | Stateless |
| CSRF | On (except Stripe webhook, H2 console) | Off — nothing is cookie-authenticated |
| Unauthenticated | Redirect to `/login` | JSON `401 unauthorized` |
| Login throttle | `LoginThrottleFilter` before form login | `ApiLoginService` checks the same `LoginThrottleService` |

Both produce a principal whose name is the user's email, so controllers on
either side resolve the `User` the same way.

API tokens (`api_tokens`, V36): 256 random bits, returned once at login, only
the SHA-256 stored. Expire after `api.token.ttl-days` (default 90), revoked by
logout, and all of a user's tokens are revoked when their password is changed
(`AccountService.changePassword`) or reset (`PasswordResetService.consumeToken`).

The client IP used for throttling comes from `X-Forwarded-For` when present
(`auth.ClientIp`) — correct only behind a trusted reverse proxy.

Admin pages are gated by an email allow-list (`admin.emails`,
`AdminAuthorizer`), not roles.

## Database

PostgreSQL 16 in production and local dev; H2 in tests. Schema is owned by
Flyway (`src/main/resources/db/migration`, currently V1–V36); migrations are
immutable once merged.

| Table | Purpose |
|---|---|
| `users` | Accounts: login email, bcrypt hash, Google subject, lifecycle timestamps, mail preferences |
| `persistent_logins` | Spring Security remember-me tokens |
| `api_tokens` | Mobile API bearer tokens (hashed) |
| `auth_events` | Login/password audit log; also the source of truth for the login throttle |
| `password_reset_tokens`, `email_verifications` | One-time tokens (hashed) |
| `mail_accounts` | Connected mailboxes: IMAP host, encrypted credential or OAuth refresh token, sync cursors, signature |
| `email_threads` | A thread per owner; carries `conversation_key` and the unread flag |
| `messages`, `message_recipients`, `participants`, `attachments` | Imported and sent mail. `attachments.blob_ref` is always null today — bytes aren't stored. |
| `subscriptions`, `plan_change_events` | Stripe subscription state and plan history |
| `teams`, `team_members`, `team_invites` | Teams and invitations |
| `saved_searches`, `digest_email_preferences` | Saved searches; weekly digest opt-out |
| `thread_notes` | Dropped in V33 |

All timestamps are `TIMESTAMP` without zone, written in UTC; the API converts
them to `…Z` instants.

## Core concept: the conversation key

Email threads are grouped into chat conversations by **who is involved**, not
by subject. `ConversationKeyService` derives a key from the set of non-owner
participant addresses; every thread with the same set shares a
`conversation_key`, and the chats list / timeline merge them. The owner's own
addresses (login email + every connected mailbox, via `OwnerAddressService`)
are excluded from the set.

## Plans

`PlanLimitService.currentPlan(user)` is the only place the effective plan is
decided: the subscription's tier while its status is `active` or `trialing`,
otherwise Free. Caps (`PlanLimits`): Free — 3 mailboxes, 500 threads; Pro — 5
mailboxes, unlimited threads; Business — unlimited. Saved searches are
unlimited on every plan.

## Background work

`@Scheduled` jobs, each behind its own feature flag: mailbox polling, IMAP
IDLE for paid tiers, weekly user digest, re-engagement and activation emails,
trial-end conversion, admin weekly digest. New mail publishes `NewMailEvent`,
which `ChatEventStream` fans out to open browser tabs over SSE.

## Build and test

See `CLAUDE.md`. `mvn -q -B verify` runs everything; one test
(`EndToEndMailboxFlowIntegrationTest`, Testcontainers) is skipped when Docker
isn't available.
