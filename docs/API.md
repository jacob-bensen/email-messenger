# API.md — canonical contract for the mobile client

**Status (2026-10-22): no mobile REST API is implemented.** The server only exposes server-rendered
pages (see `WEB.md`), the SSE stream `GET /chats/events`, and the Stripe webhook `POST /billing/webhook`.
The mobile repo (`conexus-mail-mobile`) contains no code yet. Everything below §Planned is a TBD
proposal, not behavior; implement it contract-first (update this file in the same commit).

## Existing non-HTML endpoints
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/chats/events` | session | `text/event-stream`; pushes new-mail events for the user. Payload schema TBD (read `ChatEventStream`/`NewMailEvent`). |
| POST | `/billing/webhook` | Stripe signature | Raw Stripe JSON; CSRF-exempt. Server-to-server, not for the client. |
| GET | `/actuator/health` | none | Health. |

## Planned `/api/v1` (TBD — in dependency order)
1. **Auth**: `POST /api/v1/auth/login {email,password}` → `{accessToken, refreshToken, expiresIn}`;
   `POST /api/v1/auth/refresh`; bearer auth filter beside the existing session chain; CSRF-exempt for
   `/api/**`. Google sign-in for mobile: TBD (ID-token exchange).
2. **Conversations**: `GET /api/v1/chats?page&q&mailbox&filter` → page of `ConversationListItem`
   (`key,title,initials,group,memberCount,preview,lastActivity(epochMilli),unread,threadCount,accounts,senderEmail`).
3. **Conversation**: `GET /api/v1/chats/{key}` → `ChatConversation` (members, runs of bubbles).
4. **Send**: `POST /api/v1/chats/{key}/send`, `POST /api/v1/chats`.
5. **Mailboxes / plan limits**: TBD.

Error envelope (proposed): `{ "error": "CODE", "message": "..." }`; 401 unauthenticated, 403 plan limit
(`PlanLimitExceededException`), 404 unknown key, 422 validation.
