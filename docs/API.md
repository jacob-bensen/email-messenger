# API.md — canonical API contract

**Status (2026-10-09): there is no mobile REST API yet.** The backend is a server-rendered
Thymeleaf app. This file documents (1) what machine-readable endpoints exist today and (2) the
conventions the future `/api/v1` mobile API MUST follow. Nothing under "Planned" is implemented;
do not call it from the client until a run implements it on both sides and moves it to "Implemented".

## Implemented (non-HTML) endpoints
| Method | Path | Auth | Request | Response | Errors |
|---|---|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | none | `200 {"imported": <int>}` — messages newly imported by a synchronous poll of the user's mailboxes | 302 → `/login` if unauthenticated; 403 on missing CSRF |
| GET | `/chats/events` | session | none | `text/event-stream`. Event `new-mail`, data `{"imported": <int>}`; `: ping` comment keepalives | 302 → `/login` |
| POST | `/billing/webhook` | Stripe signature header, CSRF-exempt | raw Stripe event JSON | 200 | `InvalidStripeSignatureException` → 4xx |
| GET | `/actuator/health` | public | — | Spring Boot health JSON | — |

All other routes return HTML (see `WEB.md`). Error pages render `error.html`
(`GlobalExceptionHandler`; e.g. unknown enum value → 400, oversized upload → 413).

## Planned: `/api/v1` (TBD — design constraints only)
- JSON only; `@RestController`s that call the **same services** as the Thymeleaf controllers.
- Auth: **TBD** (needs token auth; sessions+CSRF are unsuitable for a mobile client). Must not weaken
  existing form-login security.
- Candidate resources, mirroring the web workspace: conversations list (`/chats` filters: ALL/UNREAD/AWAITING/ATTACHMENTS,
  `q`, `mailbox`, `page`), conversation detail by `conversationKey`, mark-read, send/reply, mailboxes,
  plan/limits. Exact schemas **TBD**.
- Errors: single JSON error envelope **TBD**.
