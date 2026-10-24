# API contract (canonical)

Status: **the backend exposes no mobile REST API yet.** Everything below
"Existing machine-readable endpoints" is what is real today; "Planned `/api/v1`" is a
proposal (TBD) that must be agreed here *before* either repo implements it.

## Existing machine-readable endpoints (verified in code)

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| POST | `/chats/refresh` | session cookie + CSRF | none | `200 application/json` `{"imported": <int>}`-style `Map<String,Integer>` (see `ConversationController#refresh`) |
| GET | `/chats/events` | session cookie | none | `text/event-stream` (SSE) of `NewMailEvent`s (`ChatEventsController`, `ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature, CSRF-exempt | Stripe event JSON | `200` (`BillingWebhookController`, `@RestController`) |
| GET | `/actuator/health` | public | none | Spring Actuator health |

All other routes return server-rendered HTML or redirects (see `WEB.md`). They are
form-based and CSRF-protected, so they are **not** a usable mobile contract.

## Auth today
Session cookie (`JSESSIONID`) via form login at `/login`, remember-me
(`persistent_logins`), and Google OIDC (`/auth/google/start`). CSRF is on for everything
except `/h2-console/**` and `/billing/webhook`. There is no token/bearer auth.

## Planned `/api/v1` (TBD — proposal, nothing implemented)
Auth: bearer access token + refresh token (needs new Flyway migration + `SecurityFilterChain`
for `/api/**`, stateless, CSRF-exempt). Candidate first slice for a mobile client:
- `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`
- `GET /api/v1/chats` (conversation list: key, participants, last message preview, unread)
- `GET /api/v1/chats/{key}/messages` (bubble runs, same data as `ChatService`/`ConversationService`)
- `POST /api/v1/chats/{key}/send` (delegates to `ReplyService`/`OutboundMessageService`)
- `GET /api/v1/chats/events` (reuse SSE)

Rule: controllers under `/api/v1` MUST call the same services the Thymeleaf controllers use
(`ChatService`, `ConversationService`, `ReplyService`, `MailAccountService`, `PlanLimitService`).

Error format (proposed): `{"error": "<code>", "message": "<text>"}` with 400/401/403/404/409/402
(plan limit → `PlanLimitExceededException`).
