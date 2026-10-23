# API CONTRACT

Status (2026-10-23): **no JSON API exists.** The backend serves only server-rendered HTML (see `WEB.md`) and one SSE stream. The mobile client (`conexus-mail-mobile`) is an empty Flutter scaffold and consumes nothing yet.

## Implemented, non-HTML surface
- `GET /chats/events` — `text/event-stream`, session auth. Emits a signal when new mail is imported for the user (`NewMailEvent`). Payload shape: TBD (read `ChatEventStream`).
- `POST /billing/webhook` — Stripe webhook, signature-verified, CSRF-exempt. Not for the client.

## Planned v1 (PROPOSED, NOT IMPLEMENTED — to be finalised before the first feature run)
Base `/api/v1`, JSON. Auth: TBD — session cookie is unsuitable for mobile; propose `POST /api/v1/auth/login` returning a bearer token (requires new token storage + a second SecurityFilterChain; CSRF off for `/api/**`).
- `GET /api/v1/conversations` — list (`ConversationListItem`).
- `GET /api/v1/conversations/{key}` — bubbles (`ChatConversation`).
- `POST /api/v1/conversations/{key}/messages` — send reply via `ReplyService`.
- `GET /api/v1/mailboxes`.
All must delegate to the same services the Thymeleaf controllers use. Error format: TBD.
