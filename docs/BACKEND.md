# Backend architecture

Classification: BACKEND repo (pom.xml, Spring Boot, Flyway `V1..V35`, Thymeleaf).
Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate**, not jOOQ
(no jOOQ in `pom.xml`, no codegen step). Java 21, Maven wrapper, PostgreSQL 16 (prod/local), H2 (tests).

## Modules (`com.emailmessenger.*`)

| Package | Role |
|---|---|
| `auth` | Form login (`/login`), Google OIDC, remember-me (JDBC token store, 30 d), registration, password reset, email verification, login throttle, auth audit events, account settings |
| `billing` | Stripe checkout/portal/webhook, plans (`FREE`/`PRO`/`BUSINESS`), `PlanLimits` (mailboxes 3/5/∞, threads 500/∞/∞), trial + win-back nudges |
| `email` | Mailbox connect (IMAP password, Gmail OAuth), `MailboxPollingService`, IMAP IDLE, Jakarta Mail import (`EmailImportService`, `MimeMessageParser`), credential encryption |
| `service` | Conversation logic: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, bubble/run view records |
| `web` | Thymeleaf controllers (`/chats`, `/mailboxes`, landing, legal, PWA), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE `ChatEventStream` |
| `domain` / `repository` | JPA entities / Spring Data repos |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams + invites, digest mail, lifecycle emails, `/admin/revenue` metrics |

## Domain
`User`, `MailAccount` (per-user mailbox, `auth_type`, signature, IMAP cursors), `EmailThread`
(owner, `conversation_key` = participant-set key), `Message` (`outbound` flag), `Participant`,
`MessageRecipient`, `Attachment` (metadata only; `blob_ref` always null), `Subscription`, `Team*`,
`SavedSearch`, `DigestEmailPreference`, `AuthEvent`, `PlanChangeEvent`.
A *conversation* (`ChatConversation`) merges every thread with the same participant set into one timeline.

## Auth model
Session cookie (`JSESSIONID`) or remember-me cookie; CSRF **enabled** (only `/h2-console/**`, `/billing/webhook` exempt).
Everything not whitelisted in `SecurityConfig` requires authentication. There is **no token/bearer auth** today.
Server is authoritative: ownership is enforced by resolving `User` from `Principal` and scoping repository queries.

## Web vs REST
There is currently **no REST/JSON API for a mobile client**. The only `@ResponseBody` endpoints are
`POST /chats/refresh` (`{"imported":n}`) and `GET /chats/events` (SSE). All chat logic sits in
services (`ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`,
`MailAccountService`) which a future `/api/v1` controller layer must reuse — controllers
(`ConversationController`) currently also hold some orchestration (mailbox scoping:
`selectMailbox`, `scopeAddress`, send validation) that should move into a service first. See `INTERNAL_TODO.md`.

## Commands
See `CLAUDE.md`. `mvn -o` fails (empty local repo); `mvn -q -B test` (online via proxy) works.
