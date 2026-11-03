# BACKEND — architecture

Repo classification: **BACKEND** (pom.xml, Spring Boot, Flyway migrations V1–V35). It also serves the website via Thymeleaf.

Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate, not jOOQ**. There is no codegen step.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Data JPA, Flyway, PostgreSQL 16 (prod/local), H2 (tests), Stripe, Jakarta Mail (IMAP/SMTP), Gmail OAuth.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, PWA, SEO, legal, demo), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `auth` | Security config, form login + Google OIDC, registration, password reset, email verification, account pages |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `email` | IMAP import/polling/IDLE, MIME parsing, Gmail OAuth, credential encryption |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationService`, `ReplyService`, `ConversationKeyService` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, weekly digests, operator metrics, lifecycle emails |

## Schema overview (Flyway `src/main/resources/db/migration`, latest V35)
Tables: users, subscriptions, plan_change_events, mail_accounts, email_threads, messages, message_recipients, participants, attachments, saved_searches, thread_notes, teams, team_members, team_invites, auth_events, email_verifications, password_reset_tokens, persistent_logins, digest_email_preferences.
Conversations are keyed by a participant-set `conversationKey`; one conversation may merge several `email_threads`.
Plans: `FREE`, `PRO`, `BUSINESS` (`domain/Plan`).

## Auth model
- Browser session auth: form login (BCrypt), remember-me cookie, optional Google OIDC (only when client registration env vars exist).
- CSRF enabled everywhere except `/h2-console/**` and `/billing/webhook`.
- Everything not on the `permitAll` list in `SecurityConfig` requires an authenticated session.
- **There is no token-based (mobile) auth yet.** See `docs/API.md`.

## Web ↔ API relationship
Today there is only the website. Controllers are server-rendered and many hold non-trivial orchestration (see `INTERNAL_TODO.md`, e.g. `ConversationController` send/reply flow). A future `/api/v1/**` layer must call the same services, so logic should move out of controllers into services before/while it is added.

## Live updates
IMAP IDLE (paid plans, `mailbox.idle.enabled`) and polling publish `NewMailEvent`; `ChatEventStream` fans out to SSE subscribers at `GET /chats/events`.
