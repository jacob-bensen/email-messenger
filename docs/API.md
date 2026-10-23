# API contract

**Status: no mobile REST API is implemented.** This file is the canonical contract; entries
marked *Implemented* exist today, *Proposed* are the planned `/api/v1` surface (not built).

## Conventions (Proposed)
- Base `/api/v1`, JSON, UTF-8. Errors: `{"error":{"code":"STRING","message":"..."}}` with
  400 validation, 401 unauthenticated, 403 plan limit / forbidden, 404, 409 conflict.
- Auth (Proposed): `POST /api/v1/auth/token` `{email,password}` → `{accessToken,refreshToken,expiresIn}`;
  `Authorization: Bearer`. Bearer requests are CSRF-exempt. Needs a new Spring Security chain.
- Server authoritative; services shared with Thymeleaf controllers.

## Implemented (non-HTML)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/chats/refresh` | session + CSRF | → `{"imported": int}` |
| GET | `/chats/events` | session | SSE stream of new-mail events (`ChatEventStream`) |
| POST | `/billing/webhook` | Stripe signature | raw Stripe JSON |
| GET | `/actuator/health` | none | health |

## Proposed `/api/v1` (first mobile slice)
| Method | Path | Maps to service |
|---|---|---|
| GET | `/chats?mailbox=&q=&cursor=` | `ConversationService` (list items: key, title, preview, unread, lastAt) |
| GET | `/chats/{key}` | conversation + bubble runs |
| POST | `/chats/{key}/send` `{body}` | `ReplyService` |
| GET | `/mailboxes` | `MailAccountService` |
| POST | `/chats/refresh` | `MailboxPollingService.refreshActiveUserNow` |

Page routes (HTML) are listed in `WEB.md`; they are not part of this contract.
