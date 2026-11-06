# Backend architecture

Repo: `email-messenger` (ConexusMail). Classified BACKEND by evidence: `pom.xml`,
Spring Boot annotations, Flyway migrations (`V1`–`V35`), Thymeleaf templates.

## Deviations from the generic target stack
- Persistence is **Spring Data JPA / Hibernate**, not jOOQ. No codegen step.
- Postgres 16 in prod/local, H2 in tests (`src/test/resources/application.yml`).
- Auth is **session cookie + CSRF** (form login, Google OIDC, remember-me). There is
  **no token/REST auth yet** — see `docs/API.md`.

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, Gmail connect, saved searches, pricing/landing, legal, PWA, SEO) + view services (`ChatService`, `ConversationListService`, `OutboundMessageService`) |
| `service` | Conversation building: `ConversationKeyService` (participant-set key), `IMTransformService`, `EmailBodyCleaner`, `ReplyService` (SMTP send), view records (`ChatConversation`, `ConversationListItem`, `BubbleRun`, `BubbleMessage`) |
| `email` | Mailbox integration: IMAP client, IDLE, polling scheduler/service, MIME parser, `EmailImportService`, Gmail OAuth, credential encryption |
| `domain` / `repository` | JPA entities / Spring Data repositories |
| `auth` | Spring Security config, registration, login throttle, Google OIDC, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimits`: FREE 3 mailboxes/500 threads, PRO 5/unlimited, BUSINESS unlimited), trial nudges |
| `team` | Team invites/members |
| `digest`, `activation`, `reengagement` | Scheduled lifecycle emails |
| `admin` | Admin revenue/funnel/churn metrics |

## Schema overview (Flyway, `src/main/resources/db/migration`)
Core: `users`, `subscriptions`, `mail_accounts` (auth type IMAP/OAuth, polling policy,
last-seen UIDs, signature), `email_threads` (owner, unread, `conversation_key`),
`messages` (message-id header, in-reply-to, sender, plain+html body, sent_at, outbound),
`participants`, `message_recipients`, `attachments` (`blob_ref` always null today),
`saved_searches`, `teams`/`team_members`/`team_invites`, auth/billing event tables.
Migrations are immutable; add a new `V{n}`.

## Key flows
- **Import:** `MailboxPollingScheduler` → `MailboxPollingService` → `ImapClient` →
  `MimeMessageParser` → `EmailImportService` (threads by Message-ID/In-Reply-To/References;
  conversation key by participant set). New mail publishes `NewMailEvent` →
  `ChatEventStream` (SSE at `/chats/events`). Paid accounts also use IMAP IDLE.
- **Send:** `ConversationController.send` → `ReplyService` (SMTP) →
  `OutboundMessageService.recordReply/recordNewEmail`.
- **Web vs API:** all current chat logic lives in services; the controller only
  resolves mailbox scope / sending account (`scopeAddress`, `sendingAccount` are private
  static helpers in `ConversationController` — must move to a service before a REST
  controller can reuse them; see INTERNAL_TODO).
