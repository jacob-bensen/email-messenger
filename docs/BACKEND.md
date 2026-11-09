# BACKEND.md — architecture (reverse-engineered 2026-11-09)

Spring Boot 3 / Java 21 / Maven. **Persistence is Spring Data JPA (Hibernate) + Flyway, not jOOQ.**
Server-rendered Thymeleaf web app ("ConexusMail"). There is **no REST/JSON API for the mobile client yet** — see `docs/API.md`.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `auth` | Spring Security config, form login + Google OIDC, registration, password reset, email verification, login throttle, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`), trial-end conversion |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, ...) |
| `repository` | Spring Data repositories |
| `email` | IMAP import/polling/IDLE, Gmail OAuth, MIME parsing, SMTP-side credentials (`CredentialEncryptor`) |
| `service` | IM transform (`IMTransformService`, `EmailBodyCleaner`), conversation keys, `ReplyService`, chat view models |
| `web` | Thymeleaf controllers: chats workspace, mailboxes, saved searches, pricing/landing/legal, PWA, SSE |
| `team`, `digest`, `activation`, `reengagement`, `admin` | teams/invites, digest emails, nudges, admin revenue |

## Schema overview (Flyway V1–V35, `src/main/resources/db/migration`)
Tables: users, persistent_logins, password_reset_tokens, email_verifications, auth_events, subscriptions,
plan_change_events, mail_accounts (IMAP password or Gmail OAuth; per-folder UID cursors), participants,
email_threads (owner, unread, `conversation_key`), messages (incl. outbound flag), message_recipients,
attachments (`blob_ref` always null: bytes not stored), saved_searches, digest_email_preferences,
teams, team_members, team_invites. `thread_notes` was dropped in V33.
Migrations are immutable once merged.

## Auth model
- Session cookie (`JSESSIONID`) via form login (`POST /login`, params `email`, `password`, `remember-me`) or Google OIDC (only when `GOOGLE_CLIENT_ID` set). 30-day remember-me.
- CSRF **enabled** except `/billing/webhook` and `/h2-console/**`.
- Everything not in the `permitAll` list in `SecurityConfig` requires authentication.
- No token/bearer auth exists. A mobile client needs one (TBD, see INTERNAL_TODO).

## Web vs API
All controllers are Thymeleaf (`@Controller`) except `BillingWebhookController` (`@RestController`) and
`POST /chats/refresh` (`@ResponseBody` JSON). Business logic lives in services (`ChatService`,
`ConversationListService`, `ReplyService`, `OutboundMessageService`, `MailboxPollingService`, `MailAccountService`);
controllers resolve `User` from `Principal` and delegate. A future REST layer must reuse these services.

## Live updates
`MailboxPollingService` publishes `NewMailEvent`; `ChatEventStream` fans out to per-user `SseEmitter`s (30 min timeout)
as SSE event `new-mail`. `ImapIdleService` (flag `mailbox.idle.enabled`) feeds it for paid plans.
