# Backend architecture

Classification: this repo (`email-messenger`, product name ConexusMail) is the **BACKEND**
(pom.xml + Spring Boot + Flyway migrations). Evidence: `pom.xml`, 35 migrations in
`src/main/resources/db/migration`, `com.emailmessenger.*` Spring packages.

Deviation from the generic target stack: persistence is **Spring Data JPA/Hibernate**, not
jOOQ (no codegen step). Web is server-rendered Thymeleaf; there is **no REST/JSON API yet**
(see `docs/API.md`).

## Packages (`com.emailmessenger`)
| Package | Role |
|---|---|
| `web` | Thymeleaf controllers (chats, mailboxes, saved searches, legal, PWA, SEO), `ChatService`, `ConversationListService`, SSE `ChatEventStream` |
| `service` | IM transform: `IMTransformService`, `EmailBodyCleaner`, `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `BubbleMessage`, `ConversationListItem`) |
| `email` | Mailbox integration: IMAP polling (`MailboxPollingService`), IDLE (`ImapIdleService`), Gmail OAuth, `EmailImportService`, `MimeMessageParser`, credential encryption |
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account settings |
| `billing` | Stripe checkout/portal/webhook, `PlanLimits` (FREE 3 mailboxes/500 threads, PRO 5/unlimited, BUSINESS unlimited) |
| `team`, `digest`, `activation`, `reengagement`, `admin` | Teams/invites, weekly digests, lifecycle emails, admin revenue metrics |
| `domain` / `repository` | JPA entities / Spring Data repositories |

## Data model (main tables)
`users`, `mail_account` (per-user IMAP/Gmail account, encrypted credentials, UID cursors),
`email_thread` (owner, subject, `conversation_key`, unread), `message`
(Message-ID/In-Reply-To headers, sender, body, sent-at), `message_recipient`, `participant`
(normalized email), `attachment` (metadata only; `blob_ref` always null today), `subscription`,
`team*`, `saved_search`, auth/billing event tables. Latest migration: V35.
A conversation is the set of messages sharing a `conversation_key` (participant-set hash),
merged across email threads.

## Auth model
Spring Security session cookie (`JSESSIONID`) + 30-day remember-me; CSRF enabled (exempt:
`/h2-console/**`, `/billing/webhook`). Form login at `/login` (param `email`,`password`);
optional Google OIDC when `GOOGLE_CLIENT_ID` is set. Unauthenticated → redirect to `/login`.
**No token/bearer auth exists** — required before a mobile client can call the server.

## Web vs. API
Controllers call services directly (no REST layer). When the JSON API is added it must reuse
`ConversationListService`, `ChatService`, `ReplyService`/`OutboundMessageService`,
`MailAccountService` rather than the controllers' model-building code. Note
`ConversationController` currently contains non-trivial logic (mailbox scoping
`selectMailbox`/`scopeAddress`, sender-account choice `sendingAccount`, attachment
conversion) that should move into services first (see INTERNAL_TODO.md).
