# API contract (canonical)

> **Status 2026-10-18: no mobile REST API is implemented.** Everything below §1 is the contract-to-be and is **TBD**.
> Do not implement client code against §2 until a feature run defines and implements it on both sides.

## 1. Existing non-HTML endpoints (session-cookie auth, CSRF where noted)
| Method / path | Auth | Description |
|---|---|---|
| `POST /chats/refresh` | session + CSRF | Synchronously polls the caller's mailboxes. Response `200 {"imported": <int>}` |
| `GET /chats/events` | session | Server-sent events. Event `new-mail`, data `{"imported": <int>}`; comment `ping` keep-alives |
| `POST /billing/webhook` | none (Stripe signature header `Stripe-Signature`), CSRF-exempt | Raw Stripe JSON. `400` missing/invalid signature, `503` not configured |

## 2. Planned mobile API (TBD)
Not designed. Open decisions: token auth scheme (session cookies + CSRF are unsuitable for a native client), `/api/v1`
prefix, error envelope, pagination for conversation list (`/chats` currently pages with `page` and `PAGE_SIZE`), and which
service-layer methods back each endpoint (`ConversationListService`, `ChatService`, `ReplyService`, `OutboundMessageService`).
Candidate first slice: login → conversation list → open conversation → send message.
