# API.md — canonical API contract

**Status (2026-10-15): there is NO mobile REST API implemented.** The only
machine endpoint is the Stripe webhook. The mobile client cannot talk to the
server yet. This file is the contract both repos must follow; sections marked
PLANNED are proposals, not behavior — no code implements them, and they may be
revised before the first implementation run (contract first).

## Implemented machine endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | `BillingWebhookController`; invalid signature → `InvalidStripeSignatureException` |
| GET | `/chats/events` | session cookie | SSE; emits new-mail events for the user's chats (`ChatEventStream`) |
| GET | `/actuator/health` | public | health |

Everything else is HTML (see `WEB.md`). Web forms use session cookie + CSRF.

## PLANNED: `/api/v1` (first slice: auth + read chats)
Conventions: JSON, UTF-8; timestamps as UTC epoch millis (matches the existing
`sentAtEpochMilli`/`lastActivityEpochMilli` view helpers); server authoritative;
errors as `{"error":{"code":"...","message":"..."}}` with HTTP 400/401/403/404/409/429.

Auth (TBD decision): Bearer token issued by `POST /api/v1/auth/login`
(`{email,password}` → `{accessToken, expiresAt}`), chain separate from the
session/CSRF chain.

Resources, mirroring existing view records:
- `GET /api/v1/chats?mailbox=&filter=ALL|UNREAD|AWAITING|ATTACHMENTS&q=&page=` →
  `{items:[ConversationListItem], counts:{all,unread,awaiting,attachments}}` where
  item = `{key,title,initials,group,memberCount,preview,lastActivityEpochMilli,unread,threadCount,accounts[],senderEmail}`
- `GET /api/v1/chats/{key}?mailbox=` → `{key,title,initials,members:[{email,displayName,initials,label,signatureHtml}],threadCount,runs:[...]}`;
  marks the conversation read (as the web does). Bubble =
  `{messageId,bodyHtml,sentAtEpochMilli,attachments[],outbound,replyToMessageId}`.
- `POST /api/v1/chats/{key}/send` → body, optional subject, `asReply`; same plan limits as web.
Run shape (`SenderBubbleRun`) and attachment shape: TBD — read the records when implementing.

## Error codes
TBD (the web layer uses `GlobalExceptionHandler` HTML pages and `PlanLimitExceededException`; map to JSON when the API exists).
