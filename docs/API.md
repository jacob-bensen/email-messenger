# API contract (canonical)

Status: **no REST API is implemented yet.** The only machine-oriented endpoints today are listed below.
Planned `/api/v1/**` endpoints are specified here BEFORE implementation; mark each with its status.
Both repos must match this file exactly.

## Existing non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream`; emits new-mail events (`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | Stripe events; CSRF-exempt |
| GET | `/actuator/health` | none | liveness |
| GET | `/manifest.webmanifest`, `/sw.js`, `/icons/*` | none | PWA |

## Planned `/api/v1` (TBD — not implemented)
Conventions (proposed): JSON, bearer-token auth (`Authorization: Bearer <token>`), errors as
`{"error": {"code": "STRING", "message": "STRING"}}` with 400/401/403/404/409/413/429 as appropriate.

| Status | Method | Path | Maps to service |
|---|---|---|---|
| TBD | POST | `/api/v1/auth/token` | `UserService` / auth |
| TBD | GET | `/api/v1/chats` | `ConversationListService` |
| TBD | GET | `/api/v1/chats/{key}` | `ChatService` |
| TBD | POST | `/api/v1/chats/{key}/send` | `ReplyService` / `OutboundMessageService` |
| TBD | GET | `/api/v1/mailboxes` | `MailAccountService` |

Request/response schemas: TBD — define here first when the first endpoint is built.
