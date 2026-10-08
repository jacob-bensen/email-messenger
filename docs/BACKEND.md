# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-08. Unknowns are marked TBD.

**Stack (actual):** Java 21, Spring Boot 3, Spring Data **JPA/Hibernate** (not jOOQ),
Flyway, PostgreSQL 16 (H2 in tests), Thymeleaf, Spring Security, Stripe, Jakarta Mail.
`ddl-auto: validate` — the Flyway schema is authoritative.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf `@Controller`s, view-model services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `OnboardingService`), SSE (`ChatEventStream`), `GlobalExceptionHandler` |
| `service` | Conversation logic: `ConversationKeyService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService` (SMTP send), bubble/run view records |
| `email` | IMAP/Gmail import: `MailboxPollingService`, `ImapIdleService`, `EmailImportService`, `MimeMessageParser`, `MailAccountService`, `CredentialEncryptor` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `auth` | Form login, Google OIDC, registration, password reset, email verification, login throttle |
| `billing` | Stripe checkout/portal/webhook, plan limits (`Plan`: FREE/PRO/BUSINESS) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, lifecycle emails, `/admin/*` metrics |

## Domain / schema (Flyway V1..V35)
Tables: `users`, `subscriptions`, `plan_change_events`, `mail_accounts`
(auth_type PASSWORD|OAUTH, last_seen_uid, last_seen_sent_uid, polling policy, signature),
`email_threads` (owner, unread, `conversation_key`), `messages` (incl. outbound flag),
`message_recipients`, `participants`, `attachments` (`blob_ref` always null today),
`saved_searches`, `digest_email_preferences`, `teams`, `team_members`, `team_invites`,
`password_reset_tokens`, `email_verifications`, `auth_events`, `persistent_logins`.
A *conversation* is not a table: it is the set of `email_threads` sharing a
`conversation_key` (hash of the participant set) for one owner.

## Auth model
- Session cookie (`JSESSIONID`) + persistent remember-me (30 d), form login at `POST /login`
  (`email`, `password`), optional Google OIDC (only when `GOOGLE_CLIENT_ID` is set).
- CSRF is **enabled** (exempt: `/h2-console/**`, `/billing/webhook`).
- Everything not on the permit-all list in `auth/SecurityConfig` requires authentication;
  unauthenticated requests are redirected to `/login` (HTML), never 401 JSON.
- **There is no token/bearer auth, so a mobile client cannot authenticate today.** See INTERNAL_TODO.

## Web vs REST
All user-facing functionality is server-rendered HTML. The only JSON/stream-ish
endpoints are `GET /chats/events` (SSE) and `POST /billing/webhook`. There is **no REST API**
for the mobile client yet; see `docs/API.md` for the planned v1 contract.
Controllers resolve the user via `UserService.requireByEmail(principal.getName())` and
delegate to services; a future `/api/v1` layer must reuse the same services
(`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`).
Note: some orchestration (send flow: `ReplyService` + `OutboundMessageService`, plan/limit checks)
lives in `ConversationController` and must be extracted to a service before an API reuses it.

## Config
Env vars: `JDBC_DATABASE_*`, `MAIL_*`, `GOOGLE_CLIENT_ID/SECRET`, `STRIPE_*`, credential
encryption key + salt (see `application.yml`). Profiles: `local`, `prod`; tests use H2.
Docker and GitHub Actions were deliberately removed (owner decision 2026-06-11) — do not re-add.
