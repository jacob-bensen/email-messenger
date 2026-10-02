# API.md — canonical API contract

> **Status (2026-10-02): there is NO JSON/REST API for the mobile client yet.**
> The only machine-facing HTTP surface is the Stripe webhook. Everything else
> is a server-rendered, cookie-session + CSRF web app (see `WEB.md`). The
> mobile repo (`conexus-mail-mobile`) is an empty scaffold and calls nothing.
> This file is the place where `/api/v1` is specified **before** it is built.

## Existing machine-facing endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header `Stripe-Signature` (CSRF-exempt, public) | Body: raw Stripe event JSON. `400` if header missing / signature invalid. |
| GET | `/chats/events` | session cookie | `text/event-stream`; emits new-mail events per user. Payload shape TBD (see `NewMailEvent`). |
| GET | `/actuator/health` | public | Spring Boot health. |

## Planned: `/api/v1` (NOT IMPLEMENTED — design target)
Decisions still TBD: token auth scheme (bearer access + refresh token vs.
session), error envelope, pagination. Proposed first slice, all server
authoritative and delegating to the existing services:

| Method | Path | Maps to |
|---|---|---|
| POST | `/api/v1/auth/token` | email+password → token pair (TBD) |
| GET | `/api/v1/chats?page&q&mailbox&filter` | `ConversationListService.list/counts` |
| GET | `/api/v1/chats/{key}` | `ChatService` / `ChatConversation` |
| POST | `/api/v1/chats/{key}/send` | send logic (extract from `ConversationController.send`) |

Response models would mirror the records `ConversationListItem`,
`ChatConversation`, `ChatMember`, `BubbleRun`, `BubbleMessage`. Exact JSON
schemas are TBD until the first slice is chosen; do not implement the client
against this table until it is promoted out of "Planned".
