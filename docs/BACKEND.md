# BACKEND.md — architecture (reverse-engineered 2026-10-04)

Spring Boot 3 / Java 21 / Maven. **Persistence is Spring Data JPA + Hibernate
(`ddl-auto: validate`) on PostgreSQL 16 (H2 in tests) with Flyway — not jOOQ.**
No codegen step exists. Product name: ConexusMail.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats` workspace, mailboxes, pricing, SEO, PWA shell), `ChatService`, `ConversationListService`, `OutboundMessageService`, `GlobalExceptionHandler`, SSE (`ChatEventStream`) |
| `service` | Email → IM transform (`IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ConversationKeyService`), `ReplyService` (SMTP), view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`…) |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, `MimeMessageParser`, `CredentialEncryptor` |
| `auth` | `SecurityConfig`, form login, Google OIDC, password reset, email verification, login throttling, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digests, lifecycle emails, operator metrics |
| `domain`, `repository` | JPA entities / Spring Data repositories |

## Domain / schema (35 Flyway migrations, `V1`…`V35`, immutable)
`participants` (unique normalized email), `email_threads` (root Message-ID,
owner, unread, `conversation_key` from V32), `messages` (+ `message_recipients`,
`attachments`; `outbound` flag V30), `users`, `subscriptions`, `mail_accounts`
(IMAP/Gmail-OAuth, polling policy, last-seen UIDs, signature), `saved_searches`,
`teams`/`team_members`/`team_invites`, token tables (password reset, email
verification), `auth_events`, `plan_change_events`, `digest_email_preferences`,
`persistent_logins` (remember-me, created by `JdbcTokenRepositoryImpl`).
A **conversation** = all messages sharing a participant-set `conversationKey`,
merged across threads. Attachments: metadata only (`blob_ref` always null).

## Auth model
Browser-session auth: form login (`/login`, params `email`/`password`),
optional Google OIDC (only if `GOOGLE_CLIENT_ID` set), 30-day persistent
remember-me cookie, CSRF **enabled** (exempt: `/h2-console/**`,
`/billing/webhook`). Everything not on the permit-list in `SecurityConfig`
requires an authenticated session. **There is no token/Bearer auth** — a
mobile client cannot authenticate today (see INTERNAL_TODO).

## Web vs REST
There is **no REST API**. All controllers return Thymeleaf views. The only
non-HTML responses: `POST /chats/refresh` (JSON `{imported}`),
`GET /chats/events` (SSE), `POST /billing/webhook`, PWA/SEO assets. See
`docs/API.md` and `docs/WEB.md`.

Business logic is partly in controllers (mailbox scoping `selectMailbox` /
`scopeAddress` / `sendingAccount` and the send-vs-reply branching live as
private methods in `web/ConversationController`) and must be extracted into a
service before a REST controller can share it.
