# Backend architecture

Repo classification: **BACKEND** (Spring Boot 3, Java 21, Maven, Flyway, JPA, Thymeleaf).
Sibling: `conexus-mail-mobile` (Flutter client — currently an empty scaffold).

> Deviation from the generic routine template: persistence is **Spring Data JPA/Hibernate**
> (`ddl-auto: validate`), not jOOQ. There is no jOOQ codegen step.

## Modules (`com.emailmessenger.*`)

| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, `/searches`, pricing/landing/legal/PWA/SEO), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `service` | Conversation building: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService` (participant-set key), `ReplyService` (SMTP send) |
| `email` | Mailbox integration: IMAP (`JakartaImapClient`), IDLE (`ImapIdleService`), polling (`MailboxPollingService/Scheduler`), Gmail OAuth, MIME parsing, `EmailImportService`, `CredentialEncryptor` |
| `auth` | Spring Security config, registration/login/reset/verify, Google OIDC, login throttling, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (Free 3 mailboxes/500 threads; Pro 5/unlimited; Business unlimited), banners, trial nudges |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, weekly digest, lifecycle emails, admin revenue/retention metrics (`/admin/**`, allow-listed emails) |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Schema overview (Flyway `V1`–`V35`, immutable once merged)

Core: `email_thread` (owner, `conversation_key`, unread), `message` (incl. outbound flag, V30),
`message_recipient`, `participant` (deduped by normalized email), `attachment` (`blob_ref` always null today).
Accounts: `users` (Google subject, password_set), `mail_account` (encrypted IMAP creds, `auth_type`
PASSWORD|OAUTH, `last_seen_uid`, `last_seen_sent_uid`, polling policy, signature), `subscription`,
`plan_change_events`, `teams`/`team_members`/`team_invites`, `saved_searches`,
`digest_email_preferences`, `password_reset_tokens`, `email_verifications`, `auth_events`,
`persistent_logins` (remember-me).

## Auth model

- Browser session (JSESSIONID) via form login (`/login`, params `email`/`password`) or Google OIDC
  (only when `GOOGLE_CLIENT_ID` set); remember-me cookie 30 days. **CSRF is enabled** (exempt: `/billing/webhook`, `/h2-console/**`).
- Everything except an explicit allow-list (see `SecurityConfig`) requires authentication.
- **No token/API-key auth exists.** A mobile client cannot authenticate today (see `INTERNAL_TODO.md`).

## Web vs REST

All controllers are `@Controller` returning Thymeleaf views/redirects, except:
`POST /chats/refresh` (JSON `{"imported": n}`), `GET /chats/events` (SSE), and
`POST /billing/webhook` (`@RestController`). There is **no `/api/**` surface**.
Controllers resolve the user via `UserService.requireByEmail(principal.getName())` and delegate to
services, so a future REST layer can reuse `ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`.

## Commands

See `CLAUDE.md`. Backend quality gate: `./mvnw -q -B verify` (tests run on H2).
