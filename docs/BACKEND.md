# BACKEND.md — architecture

Repo: `email-messenger` (product "ConexusMail"). Java 21, Spring Boot 3.5, Spring Data **JPA/Hibernate**
(not jOOQ — the generic routine assumed jOOQ; the code says otherwise), PostgreSQL 16 (H2 in tests),
Flyway (35 migrations, `V1`…`V35`), Maven wrapper. Thymeleaf serves the website. Base package `com.emailmessenger`.

## Modules (packages)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, email verification, password reset, login throttle, account settings. Session cookie + remember-me (30d). |
| `billing` | Stripe checkout/portal/webhook, plans (Free/Pro/Business), `PlanLimitService` enforcement, trial nudges. |
| `email` | IMAP/Gmail-OAuth mailbox connect, import (`EmailImportService`, `MimeMessageParser`), polling + IMAP IDLE, credential encryption. |
| `service` | IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`. |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/legal/PWA/SEO), SSE `ChatEventStream`. |
| `domain` / `repository` | JPA entities / Spring Data repositories. |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digest mails, operator metrics (`/admin/revenue`), lifecycle schedulers. |

## Data model (key tables)
users, subscriptions, mail_accounts (encrypted creds, polling policy), email_threads, messages,
participants, message_recipients, attachments (`blob_ref` always null — bytes not stored),
saved_searches, teams/team_members/team_invites, auth_events, plan_change_events, tokens
(verification/password reset). Conversations are keyed by `conversationKey` (participant-set hash).

## Auth model
Browser session (JSESSIONID) + CSRF on, remember-me. Everything except the allowlist in
`SecurityConfig` requires authentication. **No token/bearer auth exists — mobile cannot authenticate yet.**

## Web vs REST
The app is 100% server-rendered; the only `@RestController` is `/billing/webhook` (Stripe). There is
no JSON API. Controllers call services directly, so a future `/api/v1` layer must reuse
`ConversationListService`, `ChatService`, `OutboundMessageService`, `MailAccountService`, etc. — never the controllers.
See `API.md` for the planned contract.
