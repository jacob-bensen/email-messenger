# ConexusMail backend — architecture

Spring Boot 3.5 / Java 21 / Maven. Serves the Thymeleaf website
(`docs/WEB.md`) and the mobile REST API (`docs/API.md`) from one app, one
database and one service layer.

> The routine template assumes jOOQ; this project uses **Spring Data JPA /
> Hibernate** (`ddl-auto: validate`) with Flyway. There is no codegen step.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `api` | Mobile REST API: `/api/**` stateless bearer-token security chain, controllers, DTO records, JSON error envelope |
| `web` | Website controllers, form objects, view-model services (`ConversationListService`, `ChatService`, `OnboardingService`, `SavedSearchService`), SSE `ChatEventStream`, PWA/SEO endpoints, `GlobalExceptionHandler` (HTML errors) |
| `auth` | Website security chain (`SecurityConfig`: form login, remember-me, Google OIDC), registration, password reset, email verification, login throttle + audit, account settings |
| `service` | Mail-as-chat core: `ConversationKeyService`, `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService` (SMTP send), `ConversationLabels`, demo data |
| `email` | Mailbox integration: IMAP (Jakarta Mail) polling + IDLE, Gmail OAuth, MIME parsing, import, credential encryption, owner-address resolution |
| `billing` | Stripe checkout/portal/webhooks, `Plan` limits (`PlanLimitService`), trial conversion |
| `team` | Teams and invites |
| `digest`, `reengagement`, `activation` | Scheduled lifecycle emails (feature-flagged, off by default) |
| `admin` | Operator metrics at `/admin/revenue` (allowlisted by `admin.emails`) |
| `domain` | JPA entities |
| `repository` | Spring Data repositories |

## Data model (Flyway `src/main/resources/db/migration`, V1–V36)
- `users` (+ `persistent_logins` for remember-me), `subscriptions`,
  `plan_change_events`
- `mail_accounts` — connected mailboxes (IMAP password or Gmail OAuth;
  encrypted credentials; UID cursors for inbox and Sent)
- `email_threads` (owner, `conversation_key`, unread), `messages`
  (outbound flag), `message_recipients`, `participants`, `attachments`
  (metadata only — `blob_ref` is always null)
- `saved_searches`, `digest_email_preferences`, `teams`, `team_members`,
  `team_invites`
- Auth: `password_reset_tokens`, `email_verifications`, `auth_events`,
  `api_tokens` (V36; SHA-256 of mobile bearer tokens)

A **conversation** is not a table: it is every thread of one owner sharing a
`conversation_key` (`API.md` §4.1).

## Auth model
Two `SecurityFilterChain`s:
1. `api.ApiSecurityConfig` (`@Order(1)`, `/api/**`): stateless, CSRF off,
   `Authorization: Bearer` resolved by `BearerTokenAuthenticationFilter`
   via `ApiTokenService`; JSON 401/403. Principal: `ApiPrincipal`.
2. `auth.SecurityConfig` (everything else): session + form login at
   `/login`, 30-day remember-me, optional Google OIDC, CSRF on.

Both password paths use the same `AuthenticationManager`
(`DbUserDetailsService` + BCrypt), so `LoginAuditListener`,
`AuthFailureListener` and `LoginThrottleService` cover both.

## Website vs API
Controllers translate HTTP only; rules live in services shared by both
paths (`API.md` §6). Known exceptions — logic still inside website
controllers that must move to services before the API exposes it — are
tracked in `INTERNAL_TODO.md`.

## Configuration
`application.yml` reads secrets from env vars (`JDBC_DATABASE_*`,
`STRIPE_*`, `MAILBOX_ENCRYPTION_*`, `GOOGLE_CLIENT_*`, `MAIL_*`).
`application-local.yml` targets local Postgres. Tests use H2
(`src/test/resources/application.yml`).
