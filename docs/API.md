# API contract (canonical)

> **Status: no REST API exists yet.** The server exposes only session-cookie +
> CSRF website routes (see `docs/WEB.md`). The mobile client cannot talk to the server
> until a token-authenticated `/api/v1` is built. Everything under "Planned v1" is a
> **proposal marked TBD**, not implemented behavior. Update this file *before* building.

## Existing machine-readable endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | Response `200 {"imported": <int>}` |
| GET | `/chats/events` | session | `text/event-stream`; emits new-mail events (`NewMailEvent`) |
| POST | `/billing/webhook` | Stripe signature | Not for clients |
| GET | `/actuator/health` | none | Health |

## Existing view-model shapes (to be exposed as JSON DTOs)
- `ConversationListItem`: key, title, initials, group, memberCount, preview,
  lastActivity (UTC; also epoch millis), unread, threadCount, accounts[], senderEmail.
- `ChatConversation`: key, title, initials, members[ChatMember{email, displayName,
  initials, label, signatureHtml}], threadCount, runs[SenderBubbleRun].
- `BubbleMessage`: messageId, bodyHtml (sanitized), sentAt (epoch millis), attachments[],
  outbound, replyToMessageId.
- Send semantics (`POST /chats/{key}/send`): `body`, optional `subject`, `asReply`,
  `attachments[]`, `mailbox`; recipients = conversation members; From = selected /
  default account.
- Plans: `FREE|PRO|BUSINESS` (`PlanLimits`).

## Planned v1 (TBD — not implemented)
Auth: bearer token issued by `POST /api/v1/auth/login` (TBD: token format, refresh,
Google sign-in for mobile). Candidate endpoints: `GET /api/v1/chats`,
`GET /api/v1/chats/{key}`, `POST /api/v1/chats/{key}/messages`,
`POST /api/v1/chats/{key}/read`, `GET /api/v1/mailboxes`, `GET /api/v1/me`.
Error envelope TBD. These must reuse `ConversationListService`, `ChatService`,
`ReplyService`, `OutboundMessageService` — no logic in controllers.
