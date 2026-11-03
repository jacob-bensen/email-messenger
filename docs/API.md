# API.md — canonical API contract

> **STATUS: PROPOSED — NOT IMPLEMENTED.** As of 2026-11-03 the backend exposes no REST/JSON
> API; only the HTML website. Nothing below exists in code. Both repos must treat this as a
> design target; the first feature run that implements an endpoint must move it from
> "Proposed" to "Implemented" here in the same commit.

## Conventions (proposed)
- Base path `/api/v1`, JSON, UTF-8, timestamps as epoch millis UTC (matches existing
  `*EpochMilli()` accessors on view records).
- Auth: `Authorization: Bearer <accessToken>` (new; session+CSRF is unsuitable for mobile).
- Errors: `{ "error": "<code>", "message": "<text>" }`; codes: `unauthorized` 401,
  `forbidden` 403, `not_found` 404, `validation_failed` 422, `plan_limit_exceeded` 402
  (carries `kind`: THREAD_COUNT | MAILBOX_COUNT, from `PlanLimitKind`).

## Proposed endpoints (v1 slice 1: read-only chats)
| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/token` | `{email,password}` → `{accessToken,refreshToken,expiresAt}` |
| POST | `/api/v1/auth/refresh` | `{refreshToken}` → new pair |
| GET | `/api/v1/chats?q&filter&mailbox&page` | → `{items: ConversationListItem[], page, counts}` |
| GET | `/api/v1/chats/{key}` | → `ChatConversation` (marks read) |
| POST | `/api/v1/chats/{key}/messages` | `{body, asReply, subject?}` → 201 `BubbleMessage` |

## DTOs (mirror existing service records)
- `ConversationListItem`: key, title, initials, group, memberCount, preview,
  lastActivityEpochMilli, unread, threadCount, accounts[], senderEmail.
- `ChatConversation`: key, title, initials, members[ChatMember], threadCount, runs[BubbleRun].
- `ChatMember`: email, displayName, initials, label, signatureHtml.
- `BubbleRun`: sender(SenderView), messages[BubbleMessage], outbound.
- `BubbleMessage`: messageId, bodyHtml, sentAtEpochMilli, attachments[], outbound, replyToMessageId.
  (Attachments: JSON shape TBD; bytes are not stored — `blob_ref` is always null.)
