# BACKEND — ConexusMail server

Spring Boot 3 / Java 21 monolith. It serves the Thymeleaf website and (planned) the REST API for the
mobile client from one process, one database, one service layer.

> Reality check (2026-11-01 audit): the target-stack brief mentions jOOQ. **This repo uses Spring Data
> JPA (Hibernate) + Flyway, not jOOQ.** There is no codegen step. No `/api/**` REST layer exists yet.

## Modules (`com.emailmessenger.*`)
| Package | Role |
|---|---|
| `auth` | Form login + Google OIDC, registration, password reset, email verification, account pages, login throttling, remember-me (JDBC token repo). `SecurityConfig` is the single filter chain. |
| `billing` | Stripe checkout/portal/webhook, plan limits (`PlanLimitService`, kinds: THREAD_COUNT, MAILBOX_COUNT, SAVED_SEARCH_COUNT), trial nudges. Plans: Free/Pro/Business. |
| `email` | Mailbox integration: IMAP (password) + Gmail OAuth, polling (`MailboxPollingService`, `@Scheduled`), IMAP IDLE (paid tiers), MIME parsing, import, credential encryption. |
| `service` | Conversation logic: `ConversationService`, `IMTransformService`, `EmailBodyCleaner` (quote/signature stripping), `ConversationKeyService`, `ReplyService`, view records (`ChatConversation`, `ConversationListItem`, `BubbleMessage`…). |
| `web` | Thymeleaf controllers + `ChatService`, `ConversationListService`, `OutboundMessageService`, SSE (`ChatEventStream`), PWA, SEO, legal. |
| `team`, `digest`, `admin`, `activation`, `reengagement` | Teams/invites, digest email, admin metrics, lifecycle emails. |
| `domain` / `repository` | JPA entities / Spring Data repos. |

## Data model (Flyway V1..V35, `src/main/resources/db/migration`)
`User` ← `MailAccount` (host, port, ssl, username, encrypted password / OAuth, `lastSeenUid`,
`lastSeenSentUid`, polling state, `signature`) ; `EmailThread` (owner, subject, `conversationKey`,
`unread`) → `Message` (messageIdHeader, inReplyTo, sender `Participant`, bodyPlain/Html, sentAt,
`outbound`) → `MessageRecipient`, `Attachment` (`blobRef` always null: bytes not stored);
`Subscription`/`PlanChangeEvent`, `Team`/`TeamMember`/`TeamInvite`, `SavedSearch`, auth/verification/reset
tokens. A **conversation** is not an entity: it is the set of `EmailThread`s sharing a
`conversationKey` (hash of the participant set), merged into one timeline.

## Auth model
Session cookie (`JSESSIONID`) + optional persistent remember-me (30d). CSRF ON (only `/billing/webhook`
and H2 console exempt). Public paths are an allow-list in `SecurityConfig`; everything else requires auth.
**No token/API-key auth exists** — required before a mobile client can talk to the server (see INTERNAL_TODO).

## Web vs REST
All current endpoints are HTML (see WEB.md); only `POST /billing/webhook` returns non-HTML. Controllers
look up the user via `userService.requireByEmail(principal.getName())` and delegate to services. When the
`/api/v1` layer is added it must call the same services (`ChatService`, `ConversationListService`,
`OutboundMessageService`, `MailAccountService`, `BillingService`), never the controllers.
