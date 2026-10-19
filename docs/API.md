# API contract

Status: **there is no REST API for a mobile client yet.** ConexusMail is a
server-rendered Thymeleaf app with session-cookie auth (see `BACKEND.md`).
This file lists what machine-consumable endpoints exist today and what is TBD.
Nothing below "Proposed" is implemented.

## Existing non-HTML endpoints

| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | `200 application/json` `{"imported": <int>}` — newly imported message count after a synchronous poll of the caller's mailboxes (`ConversationController.refresh`). |
| GET | `/chats/events` | session | `text/event-stream` (SSE). Live new-mail events from `ChatEventStream` (`ChatEventsController`). Event payload schema: TBD (read `NewMailEvent`). |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | Stripe webhook receiver; not for clients. |
| GET | `/actuator/health` | public | Spring Actuator health. |
| GET | `/manifest.webmanifest`, `/sw.js` | public | PWA shell. |

Everything else returns HTML (see `WEB.md`) and relies on form posts with a
CSRF token and a `JSESSIONID`/remember-me cookie.

## Error handling

`GlobalExceptionHandler` renders `error.html`. No JSON error envelope exists. TBD.

## Proposed (not implemented)

A mobile client needs a token-authenticated JSON API under `/api/v1/` backed by
the **same services** the web controllers use (`ChatService`,
`ConversationListService`, `ReplyService`, `MailAccountService`). Candidate
surface, to be specified here *before* any code: auth (login/refresh), list
conversations, get conversation (bubble runs), send reply, mailboxes list/sync,
account/plan status. Open decisions: bearer-token scheme vs. session cookie,
pagination shape, error envelope.
