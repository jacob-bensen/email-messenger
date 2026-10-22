# BACKEND

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf
website + background mail import. **No JSON REST API for a mobile client
exists yet** (see `docs/API.md`).

Classification note: this repo was identified as BACKEND by evidence
(`pom.xml`, Spring annotations, Flyway migrations V1–V34). The routine's
assumed stack differs from reality: persistence is **Spring Data JPA**
(not jOOQ); there is no jOOQ codegen. Treat JPA entities as the schema model.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/landing/PWA/SEO), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `service` | Conversation building, IM transform (`IMTransformService`, `EmailBodyCleaner`), `ReplyService`, `ConversationKeyService` |
| `email` | IMAP (`JakartaImapClient`, IDLE), Gmail OAuth, `EmailImportService`, `MimeMessageParser`, polling scheduler, `CredentialEncryptor` |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings, login throttle, audit events |
| `billing` | Stripe checkout/portal/webhook, `PlanLimitService`, trial nudges |
| `team` | Team invites/membership |
| `digest`, `reengagement`, `activation` | Scheduled lifecycle emails |
| `admin` | `/admin/revenue` operator metrics, win-back outreach |
| `domain` / `repository` | JPA entities / Spring Data repos |

## Schema
Flyway migrations `src/main/resources/db/migration/V1..V34` (immutable once
merged). Core tables: users, subscriptions, mail_accounts, email_thread,
message (+ recipients, attachments; `blob_ref` currently always null),
participant, saved_search, teams/invites, auth_events, plan_change_events.
Conversations are keyed by participant-set `conversation_key` (V32).
Prod: PostgreSQL 16; tests: H2.

## Auth model
- Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`,
  `password`, optional `remember-me`) or Google OIDC (only when
  `GOOGLE_CLIENT_ID` is set). Remember-me 30 days (persistent token repo).
- **CSRF is enabled** for all routes except `/h2-console/**` and
  `/billing/webhook`.
- Everything not in the permit-all list (see `SecurityConfig`) requires
  authentication. No bearer-token / API-key auth exists. Admin pages are
  gated by `AdminAuthorizer` (`ADMIN_EMAILS`).

## Web vs API
All HTTP surface today is web. Controllers delegate to services; the only
JSON-returning endpoints are `POST /chats/refresh` and `POST /billing/webhook`.
See `docs/WEB.md` and `docs/API.md`.

## Background jobs
Mailbox polling scheduler + IMAP IDLE (paid tiers, `mailbox.idle.enabled`),
weekly digests, activation/reengagement/trial-end emails, admin weekly digest.

## Other planning docs
`CLAUDE.md` (roadmap), `master/PLAN.md`, `master/BACKLOG.md`,
`master/MASTER_ACTIONS.md`, `master/CHANGELOG.md` (long historical log).
