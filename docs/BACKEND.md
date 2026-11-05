# BACKEND.md — architecture

Repo classification: **BACKEND** (evidence: `pom.xml`, Spring Boot annotations, Flyway migrations
in `src/main/resources/db/migration`). Sibling repo: `conexus-mail-mobile` (Flutter client; empty
scaffold as of 2026-11-05).

> Deviation from the generic routine template: persistence is **Spring Data JPA/Hibernate**, not
> jOOQ. There is no jOOQ codegen. Match the existing JPA style.

## Stack
Java 21, Spring Boot 3, Thymeleaf, Spring Data JPA, Spring Security (form login, Google OIDC,
remember-me via JDBC token repo, BCrypt), Flyway, PostgreSQL 16 (H2 in tests), Stripe, Jakarta Mail.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, pricing, PWA, SEO, legal), `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`) |
| `service` | Conversation/IM transform: `ConversationService`, `IMTransformService`, `EmailBodyCleaner`, `ReplyService`, `ConversationKeyService`; view records (`ChatConversation`, `BubbleRun`…) |
| `domain` | JPA entities (User, MailAccount, EmailThread, Message, Participant, Attachment, Subscription, Team…) |
| `repository` | Spring Data repositories |
| `email` | IMAP/Gmail integration: import, polling, IDLE, credential encryption |
| `auth` | Security config, registration, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, `PlanLimitKind`) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, digests, lifecycle emails, operator metrics |

## Key concepts
- **Conversation**: a texting-style view keyed by `conversationKey` (participant set), merged across
  email threads. Built by `ConversationService`/`ChatService`; list by `ConversationListService`
  (filters `ALL|UNREAD|AWAITING|ATTACHMENTS`, paging, free-text query, per-mailbox scope).
- **Mailbox scope**: optional `mailbox` param selects one connected `MailAccount`; otherwise the "Hub" (all).
- **Plan limits**: enforced in services via `PlanLimitService` (Free/Pro/Business).

## Schema
Flyway migrations `V1`…latest in `db/migration` (immutable once merged). See files for DDL; tables
map 1:1 to `domain` entities.

## Auth model
Session cookie + CSRF (only `/h2-console/**` and `/billing/webhook` exempt). Public routes are an
allow-list in `auth/SecurityConfig`; everything else requires login. Google OIDC is enabled only when
`GOOGLE_CLIENT_ID` is set. **No bearer-token / mobile auth exists yet** (see `INTERNAL_TODO.md`).

## Web ↔ API
There is currently **no JSON REST API**. The only non-HTML responses are the SSE stream
`/chats/events`, PWA/SEO assets and the Stripe webhook. See `docs/API.md` for the planned `/api/v1`
contract and `docs/WEB.md` for pages. Rule going forward: controllers (HTML or JSON) stay thin and call
the same services (`ChatService`, `ConversationListService`, `OutboundMessageService`, `MailAccountService`).
