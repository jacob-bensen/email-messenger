# BACKEND.md — architecture

Reverse-engineered from the code on 2026-10-12. Unknowns are marked TBD.

## Classification
`email-messenger` is the BACKEND (pom.xml, Spring Boot, Flyway migrations, Thymeleaf).
`conexus-mail-mobile` is the FRONTEND (Flutter) — currently only a README stub.

## Stack (actual, differs from the generic routine template)
Java 21, Spring Boot 3, **Spring Data JPA** (not jOOQ — no codegen step), PostgreSQL 16
(H2 in tests), Flyway (`src/main/resources/db/migration`, V1…V35), Thymeleaf, Maven wrapper.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers + chat/mailbox/saved-search/PWA/SEO/legal services |
| `service` | Email → IM transform (`IMTransformService`, `EmailBodyCleaner`), `ConversationKeyService`, `ReplyService` |
| `email` | IMAP/Gmail-OAuth import, polling, IDLE, MIME parsing, credential encryption |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team…) |
| `repository` | Spring Data repositories |
| `auth` | Security config, form login + Google OIDC, account, verification, password reset |
| `billing` | Stripe checkout/portal/webhook, plan limits, trial conversion |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |

## Data model
Tables (from migrations): users, persistent_logins, auth_events, email_verifications,
password_reset_tokens, mail_accounts, participants, email_threads, messages,
message_recipients, attachments (`blob_ref` always null — bytes not stored), saved_searches,
thread_notes, subscriptions, plan_change_events, teams, team_members, team_invites,
digest_email_preferences. Conversations are keyed by a participant-set `conversationKey`.

## Auth model
Session cookie (JSESSIONID) + remember-me (30 d, persistent_logins) via Spring Security form
login (`POST /login`, params `email`, `password`) or Google OIDC. **CSRF is enabled** (only
`/h2-console/**` and `/billing/webhook` exempt). Everything outside the permitAll list in
`SecurityConfig` requires authentication. There is **no token/bearer auth**.

## Web vs REST
There is **no REST API yet**. All user-facing routes are server-rendered Thymeleaf; only
`POST /chats/refresh` (JSON `{imported:n}`), `GET /chats/events` (SSE) and
`POST /billing/webhook` return non-HTML. A mobile client therefore has nothing stable to call —
see `INTERNAL_TODO.md` and `API.md` §Planned. Controllers delegate to services
(`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`…),
which are the layer a future `/api/v1` must reuse.
