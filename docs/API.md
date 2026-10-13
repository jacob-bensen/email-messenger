# API.md — canonical API contract (mobile client ↔ server)

**Status: no REST API is implemented (2026-10-13).** The server only exposes server-rendered
HTML (see `WEB.md`), the Stripe webhook, and an SSE stream. Nothing below is implemented unless
marked ✅. This file is the place the contract will be defined *before* code is written.

## Implemented (non-HTML)
- ✅ `POST /billing/webhook` — Stripe event ingest; signature header verified; CSRF-exempt; not for clients.
- ✅ `GET /chats/events` — `text/event-stream`, session-authenticated; emits new-mail events
  (`NewMailEvent`). Event payload shape: **TBD** (see `ChatEventStream`).
- ✅ `GET /actuator/health` — public.

## Planned (v1, NOT implemented) — proposal for the first feature run
Base path `/api/v1`, JSON, `Authorization: Bearer <token>` (new; requires a token-auth filter
beside the existing session chain; CSRF not applicable to bearer requests). Error body:
`{"error":"<code>","message":"..."}`.

| Method | Path | Purpose | Backing service |
|---|---|---|---|
| POST | `/api/v1/auth/login` | email+password → token | UserService / auth |
| GET | `/api/v1/me` | profile + plan + limits | AccountService, PlanLimitService |
| GET | `/api/v1/chats?q&mailbox&filter&page` | conversation list | ConversationListService |
| GET | `/api/v1/chats/{key}` | bubble runs for one conversation | ChatService |
| POST | `/api/v1/chats/{key}/messages` | send (body, subject?, asReply) | extracted from ConversationController |
| GET | `/api/v1/mailboxes` | connected accounts | MailAccountService |

DTO schemas: **TBD** — derive from `ChatConversation`, `BubbleRun`, `ConversationListItem`.
