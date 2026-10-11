# BACKEND — architecture

Repo classification: **BACKEND** (Spring Boot 3 / Java 21, `pom.xml`, Flyway migrations V1–V35).
Sibling: `conexus-mail-mobile` (Flutter client, currently an empty stub).

> Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate**
> (`ddl-auto: validate`), not jOOQ. There is no jOOQ codegen. Match JPA conventions.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing, legal, PWA, SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`) |
| `auth` | Spring Security config, form login + Google OIDC, registration, password reset, email verification, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial nudges |
| `email` | IMAP import (`EmailImportService`, `MimeMessageParser`), polling (`MailboxPollingService`), IDLE (`ImapIdleService`), Gmail OAuth, credential encryption |
| `service` | Reply/SMTP (`ReplyService`), conversation transform, body cleaning |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams+invites, weekly digests, lifecycle emails, operator metrics |

## DB schema overview (Flyway, immutable once merged)
Core: `users`, `subscriptions`, `mail_accounts` (encrypted creds, `auth_type`, `last_seen_uid`,
`last_seen_sent_uid`, signature), `participants`, `email_threads` (owner, `conversation_key`, unread),
`messages` (incl. outbound flag), `message_recipients`, `attachments` (`blob_ref` always null today),
`saved_searches`, `teams`/`team_members`/`team_invites`, `digest_email_preferences`,
auth/billing audit tables (`auth_events`, `plan_change_events`, `password_reset_tokens`,
`email_verifications`, `persistent_logins`). Conversations are keyed by a participant-set
`conversationKey`, not by thread.

## Auth model
Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`, `password`) or Google OIDC;
remember-me cookie (30 d). **CSRF is enabled** for every POST except `/billing/webhook` and `/h2-console/**`.
All routes except the public list in `SecurityConfig` require authentication. There is **no token/bearer
auth** and no `/api/**` namespace yet — the mobile client has no supported way to authenticate (see INTERNAL_TODO).

## Web vs REST
Web pages are the only first-class surface. JSON/SSE endpoints that exist: `POST /chats/refresh`,
`GET /chats/events` (SSE), `POST /billing/webhook`. Everything else returns HTML/redirects.
See `docs/API.md` for the contract and `docs/WEB.md` for pages.
