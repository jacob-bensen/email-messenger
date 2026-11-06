# BACKEND — architecture

Spring Boot 3 / Java 21 monolith ("ConexusMail"). Server-rendered Thymeleaf
website; **no JSON REST API exists yet** (see `API.md`). Sibling repo:
`conexus-mail-mobile` (Flutter client, currently an empty scaffold).

## Stack (verified from `pom.xml` / `application.yml`)
- Spring MVC + Thymeleaf, Spring Security (form login, Google OIDC), Spring Data **JPA/Hibernate**
  (`ddl-auto: validate`) — **not jOOQ** (the routine's default assumption is wrong for this repo).
- PostgreSQL 16 prod/local, H2 in tests; Flyway migrations `src/main/resources/db/migration` (V1–V35).
- Stripe (billing), Jakarta Mail (IMAP import, SMTP send), Gmail OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers: chats workspace, mailboxes, saved searches, pricing/legal/SEO, PWA, SSE (`/chats/events`) |
| `service` | Conversation model: `ConversationService`, `ChatConversation`/`ConversationListItem` records, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService` |
| `email` | Mailbox integration: IMAP polling/IDLE, `EmailImportService`, `MimeMessageParser`, Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `auth` | Registration, login, Google OIDC, password reset, email verification, account settings, `SecurityConfig` |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), banners |
| `team` | Team invites/membership |
| `admin`, `activation`, `digest`, `reengagement` | Operator metrics (`/admin/revenue`), lifecycle emails, schedulers |

## Domain / schema overview
`User` ─< `MailAccount` (IMAP password or Gmail OAuth; cursors `last_seen_uid`, `last_seen_sent_uid`);
`User` ─< `EmailThread` (`conversation_key` = participant-set key; the /chats workspace groups threads by it)
─< `Message` (`outbound` flag, Message-ID / In-Reply-To) ─< `MessageRecipient`, `Attachment`
(`blob_ref` always null today — bytes not stored); `Participant` (deduped by email);
`Subscription` (plan FREE/PRO/BUSINESS, Stripe ids, billing period, trial/cancel tracking);
`Team`/`TeamMember`/`TeamInvite`; `SavedSearch`; `AuthEvent`; token tables (password reset, email verification);
`PlanChangeEvent`; `DigestEmailPreference`.

## Auth model
Session cookie (form login or Google OIDC) + CSRF (only `/billing/webhook` and `/h2-console/**` exempt).
Everything not whitelisted in `SecurityConfig` requires authentication. **There is no token/API-key
auth for a mobile client** — a gap recorded in `INTERNAL_TODO.md`.

## Web vs. API
All current routes are HTML/form or SSE. `POST /chats/refresh` returns a small JSON map
(`{"imported": n}`) and `/chats/{key}/pane` returns an HTML fragment. Controllers delegate to
services (`ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`), which is where a future `/api/v1` controller layer must also delegate.

Other docs: `CLAUDE.md` (conventions + roadmap), `master/CHANGELOG.md` (historic, very long) and
root `CHANGELOG.md` (new routine entries), `INTERNAL_TODO.md`.
