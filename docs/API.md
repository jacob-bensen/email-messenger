# API.md — canonical client/server contract

**Status: no mobile REST API exists yet.** Audit (2026-10-21): the only `@RestController` is
`POST /billing/webhook` (Stripe → server, not a client endpoint). Every other route returns HTML or redirects
and relies on session cookie + CSRF. The Flutter client therefore has nothing to call.

## Conventions for the future `/api/v1` (PLANNED — nothing below is implemented)
- JSON only, under `/api/v1/**`; errors as `{ "error": "<code>", "message": "<text>" }`.
- Auth: needs a decision (TBD) — stateless bearer token issued by `POST /api/v1/auth/login` is the likely choice
  because the current session+CSRF model is browser-oriented. `/api/v1/**` will need its own security chain.
- Controllers must delegate to the existing services used by the Thymeleaf pages (`ChatService`, `ReplyService`,
  `OutboundMessageService`, `ConversationListService`); no logic forked into controllers.

## Candidate first slice (see INTERNAL_TODO.md)
| Method | Path | Purpose | Backing service |
|---|---|---|---|
| POST | `/api/v1/auth/login` | email+password → token | TBD |
| GET | `/api/v1/chats` | conversation list | ConversationListService |
| GET | `/api/v1/chats/{key}` | bubbles for one conversation | ChatService |
| POST | `/api/v1/chats/{key}/send` | send reply | ReplyService / OutboundMessageService |

Request/response schemas: TBD — to be defined here before implementation.
