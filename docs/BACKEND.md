# BACKEND — architecture

Repo classification: **backend** (pom.xml, Spring Boot, Flyway, Thymeleaf). Product name: ConexusMail.
Reverse-engineered 2026-10-26; anything not verified in code is marked TBD.

## Stack (actual)
Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ — no jOOQ dependency or codegen exists), PostgreSQL 16 (H2 in tests),
Flyway (`src/main/resources/db/migration`, V1..V35), Thymeleaf, Spring Security (form login, Google OIDC, remember-me),
Stripe (`stripe-java`), Jakarta Mail (IMAP/SMTP), jsoup.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Registration, login (+throttle), Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `email` | IMAP import (`EmailImportService`, polling, IDLE), Gmail OAuth, mailbox credentials (`CredentialEncryptor`) |
| `service` | IM transform: conversations, bubbles, quote stripping, `ReplyService` (SMTP send) |
| `web` | Chat workspace, mailboxes, saved searches, pricing/landing/legal/PWA/SEO controllers, SSE stream |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |

## Data model (tables)
users, persistent_logins, email_verifications, password_reset_tokens, auth_events, mail_accounts, participants,
email_threads, messages, message_recipients, attachments, thread_notes, saved_searches, subscriptions,
plan_change_events, digest_email_preferences, teams, team_members, team_invites. Conversations are keyed by a
participant-set `conversationKey` (not a table; derived, see `ConversationKeyService`).
Attachments: `blob_ref` is always null — bytes are not stored/downloadable (roadmap item).

## Auth model
Session cookie (`JSESSIONID`) + persistent remember-me (30 days, JDBC token store). CSRF **enabled** (only `/h2-console/**`
and `/billing/webhook` exempt). Everything except the public list in `SecurityConfig` requires authentication.
**There is no token-based (JWT/bearer) auth and no `/api/**` namespace today** — a mobile client cannot authenticate
without scraping the form login. See `INTERNAL_TODO.md`.

## Web vs API
All HTTP surface is server-rendered Thymeleaf + a handful of fragments. The only JSON/stream endpoints are
`POST /chats/refresh` and the SSE `GET /chats/events` (see `API.md`). Business logic is in services
(`ChatService`, `ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailAccountService`...), but
the chat *send* orchestration (recipient list, signature, asReply branching, attachment conversion) currently lives
in `ConversationController.send` and must be extracted to a service before a REST endpoint reuses it.
