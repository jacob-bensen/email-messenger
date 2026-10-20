# API.md — canonical client/server contract

Status as of 2026-10-20: **no mobile REST API is implemented.** Section 1 documents what exists; section 2 is the
PROPOSED v1 contract the first mobile feature must build against (nothing in section 2 exists yet — implementing it
and keeping this file in sync is the job of the next feature run).

## 1. Existing machine-readable endpoints (session cookie + CSRF)
| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session, CSRF | none | `200 {"imported": int}` |
| GET | `/chats/events` | session | none | `text/event-stream` of `NewMailEvent` (payload shape TBD — read `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature, no CSRF | Stripe event JSON | `200` / `400` invalid signature |

Unauthenticated calls to any other path get a `302 /login`, not a 401.

## 2. PROPOSED `/api/v1` (not implemented)
Conventions: JSON, UTF-8; ids are server-issued; timestamps ISO-8601 UTC; server is authoritative.
Errors: `{"error": {"code": "<SNAKE_CASE>", "message": "..."}}` with codes `UNAUTHENTICATED` (401), `FORBIDDEN` (403),
`NOT_FOUND` (404), `VALIDATION_FAILED` (422), `PLAN_LIMIT_EXCEEDED` (402, includes `kind` from `PlanLimitKind`), `RATE_LIMITED` (429).

Auth (decision TBD, recommended): `POST /api/v1/auth/token {email,password}` → `{accessToken, refreshToken, expiresIn}`;
`Authorization: Bearer` on all other calls; separate stateless filter chain under `/api/**` with CSRF off.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/me` | user, plan, limits (from `PlanLimitService`) |
| GET | `/api/v1/chats?mailbox=&filter=&q=&page=` | pages of `ConversationListItem`; same service as `/chats` |
| GET | `/api/v1/chats/{key}` | `ChatConversation`: members, bubble runs, messages |
| POST | `/api/v1/chats/{key}/messages {body, asReply, subject?}` | via `OutboundMessageService`; attachments TBD (multipart) |
| GET | `/api/v1/mailboxes` | `MailAccountService.list` |
| GET | `/api/v1/chats/events` | SSE, same stream as web |

DTO field-level schemas: TBD — derive from `ConversationListItem`, `ChatConversation`, `ChatMember`, `BubbleRun`,
`BubbleMessage` when implementing, then freeze here before writing client code.
