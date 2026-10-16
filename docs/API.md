# API contract (canonical, shared with `conexus-mail-mobile`)

## Status: **NO MOBILE API IMPLEMENTED YET** (audited 2026-10-16)

The server exposes only the following machine-consumable endpoints today. All require a browser
session cookie + (for POST) CSRF token; **none are usable from a mobile client yet.**

| Method | Path | Auth | Response |
|---|---|---|---|
| POST | `/chats/refresh` | session+CSRF | `200 {"imported": <int>}` |
| GET | `/chats/events` | session | SSE `text/event-stream`; data `{"imported": <int>}`; `: ping` comments |
| POST | `/billing/webhook` | Stripe signature | Stripe-only; not for clients |

Everything else returns HTML (see `WEB.md`).

## Proposed v1 (NOT IMPLEMENTED — design target for the first API feature run)

Conventions: base path `/api/v1`; JSON; timestamps as UTC epoch millis (`...At`, matching the
existing `*EpochMilli` view helpers); errors `{"error":{"code":"...","message":"..."}}` with
401 unauthenticated, 403 plan limit/forbidden, 404, 422 validation. Auth: bearer access token
(new; stateless filter chain for `/api/**`, no CSRF), obtained via `POST /api/v1/auth/login`.

| Method | Path | Purpose | Maps to existing service |
|---|---|---|---|
| POST | `/api/v1/auth/login` | `{email,password}` → `{accessToken,expiresAt,user}` | `DbUserDetailsService` |
| GET | `/api/v1/me` | user, plan, limits | `UserService`, `PlanLimits` |
| GET | `/api/v1/chats?q&mailbox&filter&page` | `Page<ConversationListItem>` + `ChatCounts` | `ConversationListService` |
| GET | `/api/v1/chats/{key}` | `ChatConversation` (members, runs→messages) | `ChatService` |
| POST | `/api/v1/chats/{key}/messages` | `{body,asReply,subject?,mailbox?}` | **needs extraction** from `ConversationController.send` |
| POST | `/api/v1/chats` | new conversation `{to[],subject,body,mailbox?}` | **needs extraction** |
| GET | `/api/v1/mailboxes` | connected accounts | `MailAccountService` |
| POST | `/api/v1/sync` | `{imported}` | `MailboxPollingService.refreshActiveUserNow` |

DTO field names mirror the existing records `ConversationListItem`, `ChatConversation`,
`BubbleRun`, `BubbleMessage` (see `service/`). Finalize schemas in the run that implements them.
