# API contract (canonical)

> Status: **TBD — no JSON API exists yet.** The only JSON-ish endpoint is the Stripe webhook
> (`POST /billing/webhook`, Stripe-signed, not for the mobile client). Everything else is
> server-rendered HTML with session-cookie auth + CSRF, which a mobile client should not scrape.
>
> This file is the contract both repos build against. Add each endpoint here **before**
> implementing it. Nothing below is implemented unless marked `[implemented]`.

## Conventions (proposed, to be fixed by the first API feature)
- Base path `/api/v1`; JSON; UTF-8; timestamps as UTC epoch millis (matches the existing
  `lastActivityEpochMilli` / `sentAtEpochMilli` view helpers).
- Auth: bearer token (new; session cookies are not suitable). Requires a new token table +
  `SecurityFilterChain` for `/api/**` with CSRF disabled and 401 JSON (not login redirect).
- Errors: `{ "error": "<code>", "message": "<text>" }` with codes `unauthorized`,
  `forbidden`, `not_found`, `validation_failed`, `plan_limit_exceeded`, `conflict`.

## Planned first slice (mirrors existing services; shapes taken from existing view records)
| Method | Path | Backed by | Shape |
|---|---|---|---|
| POST | `/api/v1/auth/token` | `UserService` | `{email,password}` → `{token,expiresAt}` |
| GET | `/api/v1/chats?q&mailbox&filter&page` | `ConversationListService` | page of `ConversationListItem{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}` |
| GET | `/api/v1/chats/{key}` | `ChatService` | `ChatConversation{key,title,initials,members[],threadCount,runs[]}`; bubble = `BubbleMessage{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}` |
| POST | `/api/v1/chats/{key}/send` | `ReplyService`/`OutboundMessageService` | `{body, subject?, asReply?}` (attachments multipart) → created bubble |
| GET | `/api/v1/mailboxes` | `MailAccountService` | TBD |

Limits (server-authoritative, `PlanLimits`): FREE 3 mailboxes / 500 threads; PRO 5 / unlimited;
BUSINESS unlimited. Violations map to `plan_limit_exceeded`.
