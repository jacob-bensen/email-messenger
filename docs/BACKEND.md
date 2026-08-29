# Backend architecture (email-messenger)

Spring Boot 3.5 / Java 21 monolith. Serves both the ConexusMail website
(Thymeleaf, session-based auth) and — going forward — the mobile-facing
REST API under `/api/v1/*`.

## Modules (Java packages under `com.emailmessenger`)

| Package          | Role                                                                                          |
|------------------|-----------------------------------------------------------------------------------------------|
| `EmailMessengerApplication` | Boot entrypoint; exposes shared beans (`Clock`, etc.).                             |
| `activation`     | New-user activation nudges (drip emails while the user hasn't connected a mailbox).           |
| `admin`          | Admin metrics: revenue, churn, funnels, weekly digest. Guarded by `AdminAuthorizer`.          |
| `api` (new)      | Mobile-facing REST controllers (`/api/v1/*`). Delegates to the same services as `web/`.       |
| `auth`           | Users, sessions, form login, Google OIDC, password reset, email verification, throttling.     |
| `billing`        | Stripe checkout, portal, webhooks; plans (Free/Pro/Business); plan-limit enforcement.         |
| `digest`         | Weekly per-user digest emails and unsubscribe.                                                |
| `domain`         | JPA entities (`User`, `MailAccount`, `EmailThread`, `Message`, `Attachment`, `Subscription`, ...). |
| `email`          | Mailbox integration: IMAP polling, IMAP IDLE, sent-folder sync, Gmail OAuth.                  |
| `reengagement`   | Win-back and at-risk-retention outreach.                                                      |
| `repository`     | Spring Data JPA repositories.                                                                 |
| `service`        | Domain services: chat/conversation building, threading, reply, cleaning, keys.                |
| `team`           | Team invites and membership.                                                                  |
| `web`            | Thymeleaf controllers + form objects, plus one `@ResponseBody` JSON helper (`/chats/refresh`).|

## Data model (headline)

- `User` — login identity, plan, preferences.
- `MailAccount` — one connected mailbox per user (IMAP password or Gmail OAuth).
- `EmailThread` — a threaded conversation of `Message`s; grouped in the UI
  by `conversationKey` (set of participants normalized to lowercase, sorted).
- `Message` — one email + parsed bodies + attachments; direction (inbound/outbound).
- `MessageRecipient` — `TO`/`CC`/`BCC` rows joined to a `Message`.
- `Attachment` — filename, mime, size; `blob_ref` presently always null
  (attachments not yet downloadable — see the app roadmap).
- `Subscription`, `TeamInvite`, `SavedSearch`, `PasswordResetToken`,
  `EmailVerificationToken`, `AuthEvent`, `PlanChangeEvent`.

## Auth model

**Website (current):**
- Spring Security `formLogin` on `/login` (email + password, BCrypt).
- Optional Google OIDC when `GOOGLE_CLIENT_ID` is set — `oauth2Login`
  wired lazily so absent env vars don't break the app.
- Session cookie (`JSESSIONID`) + persistent `remember-me` token (JDBC).
- CSRF ON for form posts, OFF for `/h2-console/**`, `/billing/webhook`,
  and (added for the mobile API) `/api/**`.

**Mobile (planned — see `INTERNAL_TODO.md`):**
- Bearer token — either an OAuth2 password grant or a personal
  access-token endpoint that trades email+password for a token pair.
- Header: `Authorization: Bearer <token>`. Not decided yet.

## How the website and the REST API stay in sync

Both go through the SAME service layer. Controllers (`web/*Controller`
for Thymeleaf, `api/*Controller` for REST) are thin: they parse
input, delegate to services (`service/`, `email/`, `billing/`, ...),
and shape the response for their transport. Business rules live in the
service — never duplicated in a controller or template.

When adding a new mobile endpoint:
1. Identify the service method that already implements the rule (usually
   under `service/` or `web/*Service.java`).
2. Add a `@RestController` under `api/`, delegate, and return a record.
3. Update `docs/API.md` in the same commit.
4. Add matching mobile client code in `conexus-mail-mobile/lib/network/`
   + a `lib/domain/` mirror of the rule.

Never fork logic between web and API paths. If a rule needs to change,
change the shared service and update both callers.

## Migrations

Flyway migrations in `src/main/resources/db/migration/V{N}__*.sql`.
Never edit an applied migration — always add a new one. jOOQ isn't used
here; the project uses Spring Data JPA.

## Related repositories

- `conexus-mail-mobile` — Flutter client that consumes `/api/v1/*`.
  See its `docs/DESIGN.md` (product) and `docs/MOBILE.md` (client
  architecture).
