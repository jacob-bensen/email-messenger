# API.md — canonical contract

Status 2026-11-05: **no mobile REST API exists.** This file documents what is actually exposed and reserves the `/api/v1` design space. Any new endpoint MUST be added here before implementation.

## Existing non-HTML endpoints

### GET /chats/events
- Auth: session cookie. Response `text/event-stream`.
- Event `new-mail`, data `{"imported": <int>}`. Comment keep-alive every 25s (`mailbox.sse.keepalive-ms`).

### POST /billing/webhook
- Auth: none; `Stripe-Signature` header verified. CSRF-exempt.
- 200 `ok`; 400 missing/invalid signature; 500 processing error (Stripe retries); 503 webhook secret not configured.

## Planned: /api/v1 (TBD — nothing implemented)
Needs design decisions first: token auth for mobile (TBD: bearer JWT vs opaque tokens), JSON error envelope, DTOs for conversation list (`ConversationListItem`), conversation (`ChatConversation`/`BubbleRun`), send (`ReplyService`/`OutboundMessageService`), mailboxes, account/plan. See INTERNAL_TODO.md.

Everything else (HTML forms) is documented in WEB.md and is not part of the mobile contract.
