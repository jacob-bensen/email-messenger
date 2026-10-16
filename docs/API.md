# API.md — canonical API contract

> **Status: no mobile REST API exists yet.** This file is the contract both repos
> will implement. Nothing below the "Existing machine-facing endpoints" section is
> implemented; it is marked **PLANNED** and must be implemented server-first.

## Existing machine-facing endpoints (implemented)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | Stripe events (`BillingWebhookController`) |
| GET | `/chats/events` | session cookie | `text/event-stream`; emits a signal when new mail is imported for the user |
| GET | `/actuator/health` | public | health |

All other routes are HTML (see `WEB.md`) and not part of the contract.

## Conventions for the PLANNED `/api/v1`
- JSON, UTF-8, ISO-8601 UTC timestamps, ids as numbers.
- Auth: **TBD** — requires a token scheme (e.g. bearer access token issued by
  `POST /api/v1/auth/token`); session+CSRF is unsuitable for mobile.
- Errors: `{ "error": "<code>", "message": "<text>" }` with HTTP 400/401/403/404/409/422; plan-limit
  violations map `PlanLimitExceededException` → 403 `plan_limit_exceeded`.
- Handlers must call the same services as the web controllers.

## PLANNED endpoints (first slice — see INTERNAL_TODO)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/token` | email+password → token |
| GET | `/api/v1/chats` | list conversations (`ConversationListItem`) |
| GET | `/api/v1/chats/{key}` | bubbles (`ChatConversation`/`BubbleRun`) |
| POST | `/api/v1/chats/{key}/send` | reply (`ReplyService`) |
