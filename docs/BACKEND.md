# Backend architecture

Reverse-engineered from the code on 2026-11-11. Where this disagrees with the
code, the code wins — fix this file.

## What it is

One Spring Boot 3 / Java 21 application (`com.emailmessenger`, product name
**ConexusMail**) that:

1. imports a user's email over IMAP / Gmail OAuth and stores it in PostgreSQL;
2. serves the public website and the signed-in web app as server-rendered
   Thymeleaf pages (see [WEB.md](WEB.md));
3. serves a JSON REST API under `/api/v1` for the mobile client (see
   [API.md](API.md) — the canonical contract).

Persistence is **Spring Data JPA (Hibernate)** with **Flyway** migrations.
There is no jOOQ and no code generation step. Build is Maven (`./mvnw`).
Tests run on in-memory H2 (`src/test/resources/application.yml`).

## Packages

| Package        | Role |
|----------------|------|
| `domain`       | JPA entities and enums. |
| `repository`   | Spring Data repositories. `EmailThreadRepository` holds the conversation-grouping queries. |
| `email`        | Mailbox integration: IMAP client, IMAP IDLE, Gmail OAuth, MIME parsing, import, polling (`MailboxPollingService`), credential encryption, `OwnerAddressService` (which addresses count as "me"). |
| `service`      | Mail-to-chat transformation: `ConversationKeyService` (participant-set key), `ConversationService` / `IMTransformService` / `EmailBodyCleaner` (bubbles, quote stripping, signatures), `ReplyService` (SMTP send), and the view-model records. |
| `web`          | Thymeleaf controllers plus the web-facing services they share with the API: `ConversationListService`, `ChatService`, `OutboundMessageService`, `SavedSearchService`, `ChatEventStream` (SSE), PWA/SEO endpoints, `GlobalExceptionHandler` (HTML error page). |
| `api`          | REST controllers, request/response records, and `ApiExceptionHandler` (JSON error envelope) for `/api/v1`. Controllers only translate HTTP ⇄ service calls. |
| `auth`         | Registration, login, security config, login throttle + audit log, password reset, email verification, Google OIDC sign-in, account settings, and API bearer tokens (`ApiTokenService`, `ApiLoginService`, `ApiTokenAuthenticationFilter`). |
| `billing`      | Stripe checkout/portal/webhooks, plans and plan limits, trial handling. |
| `team`         | Teams and invites. |
| `digest`, `reengagement`, `activation` | Scheduled lifecycle emails, each behind an `*.enabled` flag. |
| `admin`        | Operator dashboard (`/admin/revenue`), metrics, win-back outreach. |

## How the web pages and the REST API relate

Both are thin controllers over the **same services**; neither holds business
rules of its own.

```
Thymeleaf controller (web/)  ─┐
                              ├─►  services (web/, service/, auth/, email/)  ─►  repositories  ─►  PostgreSQL
REST controller (api/)       ─┘
```

Today the shared paths are:

| Capability           | Web                         | API                           | Shared service |
|----------------------|-----------------------------|-------------------------------|----------------|
| Sign in              | `POST /login` (form)        | `POST /api/v1/auth/login`     | Spring `AuthenticationManager` → `DbUserDetailsService`; `LoginThrottleService`; `LoginAuditListener` / `AuthFailureListener` |
| Chats list + counts  | `GET /chats`                | `GET /api/v1/conversations`   | `ConversationListService.list(...)` / `.counts(...)` |
| Background refresh   | `GET /chats`                | `GET /api/v1/conversations`   | `MailboxPollingService.refreshDueForUserAsync` |

Known logic that still lives in a web controller and must move into a service
before the API can expose it (see `INTERNAL_TODO.md`): mailbox scoping, the
sending-account choice, signature appending, and recipient parsing in
`ConversationController`.

## Auth model

Two Spring Security filter chains (`auth/SecurityConfig`):

1. **`/api/**` (order 1)** — stateless. `ApiTokenAuthenticationFilter` reads
   `Authorization: Bearer <token>`, looks up `SHA-256(token)` in `api_tokens`,
   and authenticates the request as the token's user if the token is
   unexpired, unrevoked, and the user is enabled. No session, no CSRF (nothing
   on this chain authenticates from ambient browser credentials), JSON
   401/403. Only `POST /api/v1/auth/login` is public.
2. **Everything else (order 2)** — the website: form login (`/login`, email +
   BCrypt password), optional Google OIDC login (only when
   `GOOGLE_CLIENT_ID` is configured), persistent remember-me, CSRF on,
   redirect to `/login` when unauthenticated.

On both chains the authenticated principal's name is the user's **email**;
controllers resolve the `User` with `UserService.requireByEmail`.

- **Throttle:** `LoginThrottleService` derives lockout from `auth_events`
  (default 5 `LOGIN_FAILURE` rows in 15 minutes per email or per IP). Web and
  API logins write to and read from the same log.
- **API tokens:** 32 random bytes, URL-safe base64, 90-day TTL. Revoked on
  logout (one token) and on password change / reset (all of the user's
  tokens).
- **Admin:** `/admin/**` is restricted to the `ADMIN_EMAILS` allowlist
  (`AdminAuthorizer`).
- **Data isolation:** every thread/message query is filtered by the owning
  `User`.

## Database schema overview

Flyway migrations live in `src/main/resources/db/migration` (`V1` … `V36`);
they are immutable once merged. Tables:

| Area      | Tables |
|-----------|--------|
| Accounts  | `users`, `persistent_logins` (remember-me), `api_tokens` (V36), `auth_events`, `password_reset_tokens`, `email_verifications` |
| Mail      | `mail_accounts` (connected mailboxes, encrypted credentials, poll cursors, signature), `email_threads` (owner, subject, unread, `conversation_key`), `messages`, `message_recipients`, `participants`, `attachments` (metadata only — `blob_ref` is always null today) |
| Product   | `saved_searches`, `digest_email_preferences` |
| Billing   | `subscriptions`, `plan_change_events` |
| Teams     | `teams`, `team_members`, `team_invites` |

Timestamps are `TIMESTAMP` columns holding UTC wall-clock values (the app
`Clock` bean is `Clock.systemUTC()`); the API converts them to ISO-8601 `Z`
instants.

### The conversation key

`email_threads.conversation_key` is computed by `ConversationKeyService` from
the set of non-owner participants of a thread. All threads with the same
people share a key, and the chats list groups by it. It is the `key` in the
API and the `{key}` in `/chats/{key}`.

## Plans

`Plan` = `FREE` / `PRO` / `BUSINESS`; caps are in `billing/PlanLimits`
(Free: 3 mailboxes, 500 threads; Pro: 5 mailboxes; Business: unlimited) and
enforced by `PlanLimitService`.

## Configuration

`application.yml` reads env vars (`JDBC_DATABASE_*`, `STRIPE_*`,
`GOOGLE_CLIENT_*`, `MAILBOX_ENCRYPTION_*`, feature flags such as
`MAILBOX_POLLING_ENABLED`). See `.env.example` and `DEPLOY.md`. The API adds
no new configuration.
