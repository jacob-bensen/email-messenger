# API contract (canonical)

**Status: no mobile REST API exists yet.** This file is the contract both
repos code against. Endpoints marked *PLANNED* are the proposed v1 surface;
nothing below is implemented unless marked *EXISTS*.

## Conventions (PLANNED)
- Base path `/api/v1`, JSON (`application/json`), UTC epoch-millis timestamps
  (matches `*EpochMilli` in existing view records).
- Auth: `Authorization: Bearer <accessToken>` (TBD: token issuance — see
  INTERNAL_TODO #1). No cookies/CSRF for `/api/**`.
- Errors: `{ "error": "<code>", "message": "<human text>" }` with HTTP 400
  validation, 401 unauthenticated, 402/403 plan limit (`PLAN_LIMIT_EXCEEDED`),
  404, 429 throttle.

## EXISTING non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | → `{"imported": int}` |
| GET | `/chats/events` | session | SSE stream of new-mail events |
| POST | `/billing/webhook` | Stripe signature | Stripe events |
| GET | `/actuator/health` | none | health |

## PLANNED v1
| Method | Path | Maps to |
|---|---|---|
| POST | `/api/v1/auth/login` | email+password → tokens (TBD) |
| GET | `/api/v1/chats?q&mailbox&filter&page` | `ConversationListService.list` → `ConversationListItem[]` |
| GET | `/api/v1/chats/{key}` | `ChatService.buildFor` → `ChatConversation` |
| POST | `/api/v1/chats/{key}/read` | `ChatService.markRead` |
| POST | `/api/v1/chats/{key}/send` | `ReplyService` + `OutboundMessageService` |
| POST | `/api/v1/chats` | new conversation (to, subject, body) |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` |
| GET | `/api/v1/chats/events` | SSE |

`ConversationListItem` fields: key, title, initials, group, memberCount,
preview, lastActivityEpochMilli, unread, threadCount, accounts[], senderEmail.
`BubbleMessage`: messageId, bodyHtml, sentAtEpochMilli, attachments[],
outbound, replyToMessageId. Exact JSON schemas: TBD when implemented.
