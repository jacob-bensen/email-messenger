# BACKEND.md — architecture

Repo: `email-messenger` (product "ConexusMail"). Classified BACKEND by evidence: `pom.xml`, Spring Boot
annotations, Flyway migrations under `src/main/resources/db/migration` (V1–V35).

> Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate**, not jOOQ
> (`ddl-auto: validate`; Flyway owns the schema). There is no jOOQ codegen step.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Security (form login + Google OIDC + remember-me),
Spring Data JPA, Flyway, PostgreSQL (prod/local), H2 (tests), Jakarta Mail (IMAP/SMTP), Stripe Java, jsoup.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats workspace, mailboxes, pricing, legal, PWA, SEO, saved searches), `ChatEventStream` (SSE), model advices |
| `auth` | Security config, registration/login/reset/verify, Google OIDC, account settings, login throttling |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`), trial nudges |
| `admin` | `/admin/revenue` metrics, win-back outreach, weekly operator digest |
| `email` | IMAP import/polling/IDLE, MIME parsing, Gmail OAuth, credential encryption |
| `service` | Conversation building (`ChatService`-adjacent): `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleRun`, …) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team*, SavedSearch, …) |
| `repository` | Spring Data repositories |
| `team`, `digest`, `activation`, `reengagement` | Teams/invites, weekly digest, lifecycle emails |

## Schema overview (tables)
users, mail_accounts, email_threads (has `conversation_key`, owner, unread), messages (has outbound flag),
message_recipients, participants, attachments (`blob_ref` always null today), subscriptions, plan_change_events,
saved_searches, teams/team_members/team_invites, digest_email_preferences, password_reset_tokens,
email_verifications, auth_events, persistent_logins (remember-me). Migrations are immutable; add `V36+`.

## Auth model
Session cookie (`JSESSIONID`) + optional 30-day remember-me; CSRF enabled (exempt: `/billing/webhook`,
`/h2-console/**`). Everything not in the permit-list in `SecurityConfig` requires authentication. Unauthenticated
requests redirect to `/login` (HTML). **There is no token/bearer auth and no JSON API** — see `API.md`.

## Web vs REST
Today the only client is the server-rendered website; all controllers are `@Controller` returning views/redirects
(exceptions: `POST /chats/refresh` returns JSON, `GET /chats/events` is SSE, `POST /billing/webhook` is JSON).
A mobile REST layer does not exist yet (TBD, see `INTERNAL_TODO.md`). When added it must call the same services
the web controllers use; note several rules currently live inside `ConversationController` (sender-account
selection, signature appending, recipient parsing, send+record orchestration) and must be extracted first.
