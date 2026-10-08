# BACKEND

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf UI
plus background jobs. **There is no REST/JSON API for a mobile client yet**
(see `API.md`).

Deviations from the generic "target stack": persistence is **Spring Data JPA**
(not jOOQ); schema is Flyway-managed; Maven. Prod/dev DB PostgreSQL 16, tests H2.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC (`SecurityConfig`), registration, password reset, email verification, login throttle, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`), IDLE (`ImapIdleService`), Gmail OAuth, polling (`MailboxPollingService`), RFC822 parse + import (`MimeMessageParser`, `EmailImportService`), credential encryption |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService`, view models (`BubbleRun`, `ChatConversation`, ...) |
| `web` | Thymeleaf controllers, `ChatService`, `ConversationListService`, saved searches, SSE (`ChatEventStream`), PWA/SEO, onboarding |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digest emails, lifecycle emails, operator metrics (`/admin/revenue`) |

## Schema overview (Flyway `src/main/resources/db/migration`, V1..V35)
Core: `users`, `participants`, `email_threads`, `messages`, `message_recipients`,
`attachments` (`blob_ref` always null today), `mail_accounts`, `saved_searches`,
`thread_notes`. Billing: `subscriptions`, `plan_change_events`. Auth:
`email_verifications`, `password_reset_tokens`, `persistent_logins`,
`auth_events`. Teams: `teams`, `team_members`, `team_invites`. Misc:
`digest_email_preferences`. Migrations are immutable; add `V{n}__*.sql`.

## Auth model
Session cookie (form login or Google OIDC), CSRF enabled (ignored only for
`/h2-console/**` and `/billing/webhook`), remember-me via `persistent_logins`.
Everything except an explicit public allow-list requires authentication
(`SecurityConfig`). No token/bearer auth exists — **TBD** for mobile.

## Web vs API
All user-facing behavior is Thymeleaf controllers in `web`/`auth`/`billing`
etc. calling services. Business logic lives in services (`ChatService`,
`ReplyService`, `MailAccountService`, `BillingService`). The only non-HTML
streaming endpoint is SSE `GET /chats/events`.

## Other
- `master/` holds an unrelated routine's PLAN/BACKLOG/CHANGELOG (large); the
  root `CHANGELOG.md` is the sync-routine log.
- Build/test: `./mvnw test` (see `CLAUDE.md`).
